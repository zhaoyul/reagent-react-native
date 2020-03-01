(ns test.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :is-timing?
         (fn [db]
           (let [time-left (get-in db [:login :time-left])]
             (not (= 0 time-left)))))

(reg-sub
 :verify-code-btn-txt
 (fn [db]
   (let [time-left (get-in db [:login :time-left])]
     (if (= 0 time-left)
       "获取验证码"
       (str  time-left "秒后重试")))))
