(ns status-im.ethereum.subscriptions
  (:require [status-im.ethereum.eip55 :as eip55]
            [status-im.wallet.db :as wallet]
            [status-im.wallet.core :as wallet.core]
            [status-im.ethereum.transactions.core :as transactions]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]))

(fx/defn handle-signal
  [cofx {:keys [subscription_id data] :as event}]
  (if-let [handler (get-in cofx [:db :ethereum/subscriptions subscription_id])]
    (handler data)
    (log/warn ::unknown-subscription :event event)))

(fx/defn handle-error
  [cofx {:keys [subscription_id data] :as event}]
  (log/error ::error event))

(fx/defn register-subscription
  [{:keys [db]} id handler]
  {:db (assoc-in db [:ethereum/subscriptions id] handler)})

(fx/defn max-known-block
  [{:keys [db]} block-number]
  {:db (assoc db :wallet/max-known-block block-number)})

(fx/defn new-block
  [{:keys [db] :as cofx} historical? block-number accounts transactions-per-account]
  (log/debug "[wallet-subs] new-block"
             "accounts" accounts
             "block" block-number
             "transactions-per-account" transactions-per-account
             "max-known-block" (:wallet/max-known-block db))
  (when (and (>= block-number (:wallet/max-known-block db))
             (> block-number (:ethereum/current-block db)))
    (fx/merge cofx
              (cond-> {}
                (not historical?)
                (assoc :db (assoc db :ethereum/current-block block-number))

                ;;NOTE only get transfers if the new block contains some
                ;;     from/to one of the multiaccount accounts
                (not-empty accounts)
                (assoc :transactions/get-transfers
                       {:chain-tokens (:wallet/all-tokens db)
                        :addresses    accounts
                        :before-block block-number
                        :historical?  historical?}))
              (transactions/check-watched-transactions))))

(fx/defn reorg
  [{:keys [db] :as cofx} {:keys [blockNumber accounts]}]
  (log/debug "[wallet-subs] reorg"
             "accounts" accounts
             "block-number" blockNumber)
  {:db (update-in db [:wallet :transactions]
                  wallet/remove-transactions-since-block blockNumber)})

(fx/defn recent-history-fetching-started
  [{:keys [db]} accounts]
  (log/debug "[wallet-subs] recent-history-fetching-started"
             "accounts" accounts)
  {:db (transactions/update-fetching-status db accounts :recent? true)})

(fx/defn recent-history-fetching-ended
  [{:keys [db] :as cofx} {:keys [accounts blockNumber]}]
  (log/debug "[wallet-subs] recent-history-fetching-ended"
             "accounts" accounts
             "block" blockNumber)
  (fx/merge
   cofx
   {:db (-> db
            (update-in [:wallet :accounts]
                       wallet/remove-transactions-since-block blockNumber)
            (transactions/update-fetching-status accounts :recent? false)
            (dissoc :wallet/waiting-for-recent-history?
                    :wallet/refreshing-history?))
    :transactions/get-transfers
    {:chain-tokens (:wallet/all-tokens db)
     :addresses    (reduce
                    (fn [v address]
                      (let [normalized-address
                            (eip55/address->checksum address)]
                        (if (contains? v normalized-address)
                          v
                          (conj v address))))
                    []
                    accounts)
     :before-block blockNumber
     :limit        20
     :historical?  true}}
   (wallet.core/restart-wallet-service-default)))

(fx/defn new-wallet-event
  [cofx {:keys [type blockNumber accounts newTransactions] :as event}]
  (log/debug "[wallet-subs] new-wallet-event"
             "event-type" type)
  (case type
    "newblock" (new-block cofx false blockNumber accounts newTransactions)
    "history" (new-block cofx true blockNumber accounts nil)
    "maxKnownBlock" (max-known-block cofx blockNumber)
    "reorg" (reorg cofx event)
    "recent-history-fetching" (recent-history-fetching-started cofx accounts)
    "recent-history-ready" (recent-history-fetching-ended cofx event)
    (log/warn ::unknown-wallet-event :type type :event event)))
