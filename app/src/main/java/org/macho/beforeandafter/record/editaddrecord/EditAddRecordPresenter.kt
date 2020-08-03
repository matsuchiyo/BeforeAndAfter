package org.macho.beforeandafter.record.editaddrecord

import android.content.Context
import org.macho.beforeandafter.shared.data.record.Record
import org.macho.beforeandafter.shared.data.record.RecordRepository
import org.macho.beforeandafter.shared.util.LogUtil
import org.macho.beforeandafter.shared.util.SharedPreferencesUtil
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.max

class EditAddRecordPresenter @Inject constructor(val recordRepository: RecordRepository): EditAddRecordContract.Presenter {

    var view: EditAddRecordContract.View? = null

    @Inject
    lateinit var context: Context

    private lateinit var record: Record
    private var tempDate: Long = 0

    // 画像選択後、途中で保存をやめた時にその画像を削除できるようにするためのフィールド
    override var tempFrontImageFileName: String? = null
    override var tempSideImageFileName: String? = null

    val dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    override fun setDate(date: Long) {
        record = Record()
        if (date != 0L) {
            tempDate = date
            recordRepository.getRecord(date) { record ->
                if (record == null) {
                    throw RuntimeException("record shouldn't be null.")
                }

                this.record = record

                updateView()

                view?.showDeleteButton()
            }
        } else {
            tempDate = record.date
            record.weight = SharedPreferencesUtil.getFloat(context, SharedPreferencesUtil.Key.LATEST_WEIGHT)
            record.rate = SharedPreferencesUtil.getFloat(context, SharedPreferencesUtil.Key.LATEST_RATE)
        }

        tempFrontImageFileName = null
        tempSideImageFileName = null
    }

    override fun saveRecord(weight: String?, rate: String?, memo: String?) {
        record.weight = returnZeroIfEmptyOrMinus(weight)
        record.rate = returnZeroIfEmptyOrMinus(rate)
        record.memo = memo ?: ""

        if (tempFrontImageFileName != null) {
            val oldName = record.frontImagePath
            deleteIfExists(oldName)
            record.frontImagePath = tempFrontImageFileName
            tempFrontImageFileName = null /* destroy時にファイルを削除するので、その時に消さないようにnullにする */
        }

        if (tempSideImageFileName != null) {
            val oldName = record.sideImagePath
            deleteIfExists(oldName)
            record.sideImagePath = tempSideImageFileName
            tempSideImageFileName = null /* destroy時にファイルを削除するので、その時に消さないようにnullにする */
        }

        recordRepository.getRecord(record.date) { record ->
            if (record == null) {
                recordRepository.register(this.record) {
                    if (tempDate == this.record.date) {
                        view?.finish()
                        return@register
                    }
                    recordRepository.delete(tempDate) { // dateを編集した場合、古いのを消す。
                        view?.finish()
                    }
                }
            } else {
                recordRepository.update(this.record, null)
                view?.finish()
            }
        }

        SharedPreferencesUtil.setFloat(context, SharedPreferencesUtil.Key.LATEST_WEIGHT, record.weight)
        SharedPreferencesUtil.setFloat(context, SharedPreferencesUtil.Key.LATEST_RATE, record.rate)
    }

    override fun deleteRecord() {
        recordRepository.delete(record.date, null)

        view?.finish()
    }

    // NOTE: this method will be called Fragment.onResume()
    override fun takeView(view: EditAddRecordContract.View) {
        this.view = view
        updateView()
    }

    // NOTE: this method will be called Fragment.onDestoryView()
    override fun dropView() {
        deleteIfExists(tempFrontImageFileName)
        deleteIfExists(tempSideImageFileName)
        view = null
    }

    override fun setMemo(memo: String?) {
        if (memo == null) {
            return
        }
        record.memo = memo
    }

    override fun setWeight(weight: String?) {
        record.weight = weight?.toFloatOrNull() ?: 0f
    }

    override fun setRate(rate: String?) {
        record.rate = rate?.toFloatOrNull() ?: 0f
    }

    override fun onDateButtonClicked() {
        LogUtil.i(this, "dateButton.onClick")
        view?.showDatePickerDialog(Date(record.date))
    }

    override fun onDateSelected(date: Date) {
        LogUtil.i(this, "onDateSelected$date")
        record.date = date.time
        view?.setDateButtonLabel(dateFormat.format(date))
    }

    private fun isFileExists(fileName: String?): Boolean {
        return fileName != null && File(context.filesDir, fileName).exists()
    }

    private fun updateView() {
        if (isFileExists(record.frontImagePath)) {
            view?.setFrontImage(File(context.filesDir, tempFrontImageFileName ?: record.frontImagePath))
        }

        if (isFileExists(record.sideImagePath)) {
            view?.setSideImage(File(context.filesDir, tempSideImageFileName ?: record.sideImagePath))
        }

        view?.setDateButtonLabel(dateFormat.format(record.date))
        view?.setWeight(if (record.weight == 0f) "" else "%.2f".format(record.weight))
        view?.setRate(if (record.rate == 0f) "" else "%.2f".format(record.rate))
        view?.setMemo(record.memo)
    }

    private fun returnZeroIfEmptyOrMinus(value: String?): Float {
        return max(value?.toFloatOrNull() ?: 0f, 0f)
    }

    private fun deleteIfExists(fileName: String?) {
        if (fileName == null) {
            return
        }
        if (fileName.isEmpty()) {
            return
        }
        val target = File(context.filesDir, fileName)
        if (!target.exists()) {
            return
        }
        target.delete()
    }
}