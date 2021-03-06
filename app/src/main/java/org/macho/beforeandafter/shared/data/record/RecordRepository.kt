package org.macho.beforeandafter.shared.data.record

interface RecordRepository {
    fun getRecords(with: (List<Record>) -> Unit)
    fun getRecord(date: Long, with: (Record?) -> Unit)
    fun register(record: Record, with: (() -> Unit)? = null)
    fun update(record: Record, with: (() -> Unit)? = null)
    fun delete(date: Long, with: (() -> Unit)?)
}