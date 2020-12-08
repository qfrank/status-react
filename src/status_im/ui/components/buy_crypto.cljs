(ns status-im.ui.components.buy-crypto
  (:require [quo.react-native :as rn]
            [quo.design-system.colors :as colors]
            [quo.core :as quo]
            [status-im.react-native.resources :as resources]
            [status-im.ui.components.icons.vector-icons :as vector-icons]))

(defn banner [{:keys [on-close on-open]}]
  [rn/view {:style {:border-radius    16
                    :background-color (colors/get-color :interactive-02)
                    :flex-direction   :row
                    :justify-content  :space-between}}
   [rn/touchable-opacity {:style    {:padding-horizontal 8
                                     :padding-vertical   10
                                     :flex-direction     :row
                                     :align-items        :center}
                          :on-press on-open}
    [rn/image {:source (resources/get-image :empty-wallet)
               :style  {:width  40
                        :height 40}}]
    [rn/view {:style {:padding-left 16}}
     [quo/text {:weight :bold}
      "Looks like your wallet is empty"]
     [quo/text {:color :link}
      "Find a dapp to buy crypto now →"]]]
   [rn/touchable-opacity {:style    {:padding 4}
                          :on-press on-close}
    [vector-icons/icon :main-icons/close-circle {:color (colors/get-color :icon-02)}]]])
