(ns test.app
  (:require
   ["create-react-class" :as crc]
   ["react-native" :as rn]
   ["react" :as react]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [test.utils :refer [toastError]]
   [test.events]
   [test.subs]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir
(defonce ^:private themeColor "#BA2D48")

(defn- phoneNo? [str]
  (re-matches #"^1\d{10}" str))

(def ^:private state (r/atom {:phoneNum ""
                              :captcha ""
                              :recmdMobile ""}))
(def ^:private isX
  (let [{:keys [width height ]}
        (js->clj (.get rn/Dimensions "window") :keywordize-keys true) ]
    (and (> height (* 2 width))
         (= "ios" (.-OS rn/Platform)))))

(defn- getCapLabel []
  [:> rn/Text {:style #js { :fontSize 11 :color "white" } }
   @(rf/subscribe [:verify-code-btn-txt])])

(def styles
  ^js (-> {:loginBg
           {:width "100%"
            :height "100%"}
           :content
           {:flex 1
            :paddingLeft 30
            :paddingRight 30
            :paddingTop (if isX  (+ 70 80) (+ 30  80))
            :backgroundColor "transparent"}
           :t1 {:fontSize 22
                :color "white"
                :fontWeight "bold"}
           :username_input_layout
           {:width "100%"
            :height 40
            :marginTop 40
            :flexDirection "row"
            :alignItems "center"}
           :t2
           {:width 70
            :fontSize 15
            :color "white"}
           :input_username
           {:flex 1
            :fontSize 15
            :color "white"
            :paddingBottom 0
            :paddingTop 0}
           :line
           {:marginTop 5
            :marginBottom 5
            :height (.-hairlineWidth rn/StyleSheet)
            :width "100%"
            :backgroundColor "lightgray"}
           :passwd_input_layout
           {:width "100%"
            :height 30
            :marginTop 30
            :flexDirection "row"
            :alignItems "center"}
           :get_captcha
           {:width 68
            :height 28
            :backgroundColor themeColor
            :flexDirection "column"
            :justifyContent "center"
            :alignItems "center"
            :borderRadius 5}
           :t3
           {:color "white"
            :fontSize 18}
           :submitBtn {:width "90%"
                       :height 40
                       :borderRadius 5
                       :marginTop 40
                       :backgroundColor themeColor
                       :justifyContent "center"
                       :alignItems "center"
                       :alignSelf "center"}
           }
          (clj->js)
          (rn/StyleSheet.create)))



(defn- handleSubmit []
  (let [{:keys [phoneNum captcha recmdMobil]} @state]
    (if-not (phoneNo?  phoneNum)
      (toastError "请输入正确的手机号")
      (if (empty? captcha)
        (toastError "请填写验证码")
        (rf/dispatch [:login {
                              :openId nil
                              :ticket {:isTrusted true}
                              :randstr ""
                              :sourcePage "https://webt.rrs.com/pages/home/personal.html?t=15826320"
                              :orderId ""
                              :recmdMobile recmdMobil
                              :thirdpartyType ""
                              :thirdpartyId ""
                              :mobile phoneNum
                              :verifycode captcha}])))
    ))

(defn- inviterLabel []
  (let [{:keys [showInviter] } @state]
    (when showInviter
      [:> rn/View {:style (.-passwd_input_layout styles)
                   :keyboardType "numeric"
                   :returnKeyType "done"
                   :placeholderTextColor "lightgray"
                   :placeholder "请输入推荐人手机号"
                   ;;:underlineColorAndroid "#00000000"
                   :onChangeText
                   (fn [text] (swap! state conj :recmdMobile text))}])))



(defn- handleVerifycode []
  (let [{:keys [phoneNum]} @state]
    (if-not (phoneNo? phoneNum)
      (toastError "请输入正确的手机号")
      (rf/dispatch [:veriry-code {:phoneNum phoneNum} ]))))

(defn- root []
  [:> rn/ImageBackground
   {:resizeMode  "cover"
    :style (.-loginBg styles)
    :source (js/require "../assets/login-bg.jpg")}
   [:> rn/View {:style (.-content styles)}
    [:> rn/Text {:style (.-t1 styles)} (str "欢迎使用日日顺")]
    [:> rn/View {:style (.-username_input_layout styles)}
     [:> rn/Text {:style (.-t2 styles)} "+86"]
     [:> rn/TextInput {:style (.-input_username styles)
                       :placeholderTextColor "lightgray"
                       :placeholder "请输入手机号"
                       :keyboardType "numeric"
                       :returnKeyType "next"
                       :underlineColorAndroid="#00000000"
                       :onChangeText (fn [text]
                                       (prn text)
                                       (swap! state assoc :phoneNum text))}]]
    [:> rn/View {:style (.-line styles)}]
    [:> rn/View {:style (.-passwd_input_layout styles)}
     [:> rn/Text {:style (.-t2 styles)} "验证码"]
     [:> rn/TextInput {:ref "captchaInputRef"
                       :style (.-input_username styles)
                       :placeholderTextColor "lightgray"
                       :placeholder "请输入验证码"
                       :keyboardType "numeric"
                       :onSubmitEditing handleSubmit
                       :underlineColorAndroid "#00000000"
                       :onChangeText (fn [text]
                                       (prn text)
                                       (swap! state assoc :captcha text))}]
     [:> rn/TouchableOpacity
      {:disabled @(rf/subscribe [:is-timing?])
       :style    (.-get_captcha styles)
       :onPress  handleVerifycode
       :hitSlop #js { :top 10 :bottom 10 :left 20 :right 20 }}
      [getCapLabel]]]
    [:> rn/View {:style (.-line styles)}]
    [:> rn/TouchableOpacity
     {:style (.-submitBtn styles)
      :onPress handleSubmit }
     [:> rn/Text {:style (.-t3 styles)} "登陆"]]]])



(defonce root-ref (atom nil))
(defonce root-component-ref (atom nil))

(defn render-root [root]
  (let [first-call? (nil? @root-ref)]
    (reset! root-ref root)

    (if-not first-call?
      (when-let [root @root-component-ref]
        (.forceUpdate ^js root))
      (let [Root
            (crc
              #js {:componentDidMount
                   (fn []
                     (this-as this
                       (reset! root-component-ref this)))
                   :componentWillUnmount
                   (fn []
                     (reset! root-component-ref nil))
                   :render
                   (fn []
                     (let [body @root-ref]
                       (if (fn? body)
                         (body)
                         body)))})]

        (rn/AppRegistry.registerComponent "AwesomeProject" (fn [] Root))))))

(defn start
  {:dev/after-load true}
  []
  (render-root (r/as-element [root])))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (start))

