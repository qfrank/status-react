(ns status-im.ui.screens.browser.bookmarks.views
  (:require-macros [status-im.utils.views :as views])
  (:require [status-im.ui.components.keyboard-avoid-presentation :as kb-presentation]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.topbar :as topbar]
            [status-im.i18n :as i18n]
            [status-im.ui.components.toolbar :as toolbar]
            [quo.core :as quo]
            [status-im.ui.components.colors :as colors]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(views/defview new-bookmark []
  (views/letsubs [{:keys [url name]} [:get-screen-params]
                  input-name (reagent/atom name)]
    (let [edit? (not (string/blank? name))]
      [kb-presentation/keyboard-avoiding-view {:style {:flex 1}}
       [react/view {:flex 1}
        [topbar/topbar
         {:modal?        true
          :border-bottom true
          :title         (if edit? (i18n/label :t/edit-favourite) (i18n/label :t/new-favourite))}]
        [react/view {:style {:flex 1}}
         [quo/text-input
          {:container-style     {:margin 16 :margin-top 10}
           :accessibility-label :bookmark-input
           :max-length          100
           :auto-focus          true
           :show-cancel         false
           :label               (i18n/label :t/name)
           :default-value       name
           :on-change-text      #(reset! input-name %)}]
         [react/text {:style {:margin 16 :color colors/gray}}
          url]]
        [toolbar/toolbar
         {:show-border? true
          :center
          [quo/button
           {:accessibility-label :save-bookmark
            :type                :secondary
            :disabled            (string/blank? @input-name)
            :on-press            #(do (if edit?
                                        (re-frame/dispatch [:browser/update-bookmark {:url url :name @input-name}])
                                        (re-frame/dispatch [:browser/store-bookmark {:url url :name @input-name}]))
                                      (re-frame/dispatch [:navigate-back]))}
           (if edit? (i18n/label :t/save) (i18n/label :t/add-favourite))]}]]])))