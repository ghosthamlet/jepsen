(ns jepsen.txn
  "Manipulates transactions. Transactions are represented as a sequence of
  micro-operations (mops for short).")

(defn reduce-mops
  "Takes a history of operations, where each operation op has a :value which is
  a transaction made up of [f k v] micro-ops. Runs a reduction over every
  micro-op, where the reduction function is of the form (f state op [f k v]).
  Saves you having to do endless nested reduces."
  [f init-state history]
  (reduce (fn op [state op]
            (reduce (fn mop [state mop]
                      (f state op mop))
                    state
                    (:value op)))
          init-state
          history))

(defn ext-reads
  "Given a transaction, returns a map of keys to values for its external reads:
  values that transaction observed which it did not write itself."
  [txn]
  (loop [ext      (transient {})
         ignore?  (transient #{})
         txn      txn]
    (if (seq txn)
      (let [[f k v] (first txn)]
         (recur (if (or (not= :r f)
                        (ignore? k))
                  ext
                  (assoc! ext k v))
                (conj! ignore? k)
                (next txn)))
      (persistent! ext))))

(defn ext-writes
  "Given a transaction, returns the map of keys to values for its external
  writes: final values written by the txn."
  [txn]
  (loop [ext (transient {})
         txn txn]
    (if (seq txn)
      (let [[f k v] (first txn)]
        (recur (if (= :r f)
                 ext
                 (assoc! ext k v))
               (next txn)))
      (persistent! ext))))
