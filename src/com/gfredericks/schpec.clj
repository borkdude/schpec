(ns com.gfredericks.schpec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as sg]
            [clojure.set :as set]))

(defn limit-keys
  "Given a key spec form, limits its keys to a particular subset."
  [spec-form ks]
  (let [ks (set ks)]
    (s/and spec-form (fn [m] (set/subset? (set (keys m)) ks)))))

(defmacro excl-keys
  "Like [[s/keys]], but closed for extension.

  The generator for this spec will only generate maps with explicitly
  specified keys.

  The :check-keys? keyword argument (true by default) determines if
  the limitation on the keys is enforced. This is useful if you'd like
  to merge the key-specs later: [[s/merge]] assumes that the data
  satisfies the individual specs as well. You can limit the keys
  allowed in a key spec later using [[limit-keys]]."
  [& {:keys [req-un opt-un req opt check-keys?]
      :as keys-spec
      :or {check-keys? true}}]
  (let [keys-spec (dissoc keys-spec :check-keys?)
        bare-un-keys (map (comp keyword name) (concat req-un opt-un))
        all-keys (set (concat bare-un-keys req opt))]
    `(let [ks# (s/keys ~@(apply concat keys-spec))]
       (s/with-gen
         (if ~check-keys?
           (limit-keys ks# ~all-keys)
           ks#)
         (fn [] (sg/fmap (fn [m#] (select-keys m# ~all-keys)) (s/gen ks#)))))))
