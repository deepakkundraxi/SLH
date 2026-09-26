package com.slh.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ============================================================
 * CLOUD DIFF SYNC
 * ============================================================
 *
 * A store calls [onPersist] with its FULL record list every time
 * it saves to disk. This class compares it with what it last knew
 * to be in sync and pushes only the difference to Firestore:
 * new / changed records -> upsert, missing records -> delete.
 *
 * That way every mutation path of a big store (fees, payments)
 * is covered without touching each function.
 *
 * - [reset] sets the baseline WITHOUT pushing anything. Call it
 *   after loading from disk and after a pull from Firestore.
 * - [comparable] lets a store ignore derived fields (for example
 *   an installment's OVERDUE status) so they cause no writes.
 * - A failed upsert / delete is retried on the next [onPersist].
 * - clear()-style resets must call [reset] BEFORE persisting, or
 *   the empty list would delete everything in the cloud.
 *
 * ============================================================
 */
class CloudDiffSync<T>(
    private val idOf: (T) -> String,
    private val comparable: (T) -> Any? = { it },
    private val upsert: suspend (T) -> Unit,
    private val delete: suspend (String) -> Unit
) {

    private val lock = Any()

    private var known: Map<String, Any?> =
        emptyMap()

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)


    fun reset(
        records: List<T>
    ) {

        synchronized(lock) {

            known =
                records.associate {
                    idOf(it) to comparable(it)
                }
        }
    }


    fun onPersist(
        records: List<T>
    ) {

        val toUpsert = mutableListOf<T>()

        val toDelete = mutableMapOf<String, Any?>()

        synchronized(lock) {

            val current = HashMap<String, T>()

            records.forEach {
                current[idOf(it)] = it
            }

            for ((id, record) in current) {

                if (
                    !known.containsKey(id) ||
                    known[id] != comparable(record)
                ) {
                    toUpsert.add(record)
                }
            }

            for ((id, value) in known) {

                if (!current.containsKey(id)) {
                    toDelete[id] = value
                }
            }

            if (toUpsert.isEmpty() && toDelete.isEmpty()) {
                return
            }

            val next = HashMap(known)

            toUpsert.forEach {
                next[idOf(it)] = comparable(it)
            }

            toDelete.keys.forEach {
                next.remove(it)
            }

            known = next
        }

        for (record in toUpsert) {

            scope.launch {

                try {

                    upsert(record)

                } catch (e: Exception) {

                    // Forget it so the next persist retries.
                    synchronized(lock) {
                        known = known - idOf(record)
                    }

                    SLHFirebase.logSyncFailure(
                        "CloudDiffSync upsert ${idOf(record)}",
                        e
                    )
                }
            }
        }

        for ((id, oldValue) in toDelete) {

            scope.launch {

                try {

                    delete(id)

                } catch (e: Exception) {

                    // Put it back so the next persist retries the delete.
                    synchronized(lock) {
                        known = known + (id to oldValue)
                    }

                    SLHFirebase.logSyncFailure(
                        "CloudDiffSync delete $id",
                        e
                    )
                }
            }
        }
    }
}
