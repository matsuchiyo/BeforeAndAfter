package org.macho.beforeandafter.dashboard

import android.content.Context
import org.macho.beforeandafter.R
import org.macho.beforeandafter.dashboard.view.BMIClass
import org.macho.beforeandafter.dashboard.view.PhotoData
import org.macho.beforeandafter.shared.data.record.Record
import org.macho.beforeandafter.shared.data.record.RecordRepository
import org.macho.beforeandafter.shared.di.ActivityScoped
import org.macho.beforeandafter.shared.extensions.getBoolean
import org.macho.beforeandafter.shared.util.SharedPreferencesUtil
import org.macho.beforeandafter.shared.util.WeightScale
import java.util.*
import javax.inject.Inject
import kotlin.math.pow

@ActivityScoped
class DashboardPresenter @Inject constructor(): DashboardContract.Presenter {

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var recordRepository: RecordRepository

    private var view: DashboardContract.View? = null

    private lateinit var weightScale: WeightScale

    override fun reloadDashboard() {
        reloadRecords()
    }

    override fun takeView(view: DashboardContract.View) {
        this.view = view

        reloadRecords()
    }

    override fun dropView() {
        this.view = null
    }

    // MARK: - Private
    private fun reloadRecords() {
        recordRepository.getRecords { records ->
            view?.toggleEmptyView(records.isEmpty())

            this.weightScale = WeightScale(context)
            val weightUnit = weightScale.weightUnitText

            val isStartTimeCustomized = SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.CUSTOMIZE_START_TIME)
            val startTime = if (isStartTimeCustomized) SharedPreferencesUtil.getLong(context, SharedPreferencesUtil.Key.START_TIME) else 0L

            val recordsSortedByDate = records.filter { it.date >= startTime }.sortedBy { it.date }

            val weightRecordsSortedByDate = recordsSortedByDate.filterNot { it.weight == 0f }
            val weightFirstRecord = weightRecordsSortedByDate.firstOrNull()
            val weightLatestRecord = weightRecordsSortedByDate.lastOrNull()
            val weightBestRecord = weightRecordsSortedByDate.minBy { it.weight }
            val goalWeight = SharedPreferencesUtil.getFloat(context, SharedPreferencesUtil.Key.GOAL_WEIGHT)

            val showWeightProgress = !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_WEIGHT_PROGRESS)
            val elapsedDay: Int = if (weightFirstRecord == null) {
                1
            } else {
                val firstCalendar = Calendar.getInstance().also {
                    it.time = Date(weightFirstRecord.date)
                    it.set(Calendar.HOUR_OF_DAY, 0)
                    it.set(Calendar.MINUTE, 0)
                    it.set(Calendar.SECOND, 0)
                    it.set(Calendar.MILLISECOND, 0)
                }
                ((Date().time - firstCalendar.time.time) / (1000 * 60 * 60 * 24) + 1).toInt()
            }
            view?.updateWeightProgress(
                    showWeightProgress,
                    weightUnit,
                    elapsedDay,
                    weightFirstRecord?.weight?.let { weightScale.convertFromKg(it) },
                    weightBestRecord?.weight?.let { weightScale.convertFromKg(it) },
                    weightLatestRecord?.weight?.let { weightScale.convertFromKg(it) },
                    weightScale.convertFromKg(goalWeight)
            )

            val day = 1000L * 60 * 60 * 24
            val now = Date().time
            val oneWeekAgo = now - 7 * day
            val twoWeekAgo = now - 14 * day
            val thirtyDaysAgo = now - 30 * day
            val sixtyDaysAgo = now - 60 * day
            val oneYearAgo = now - 365 * day
            val twoYearAgo = now - 365 * 2 * day

            val theYearBeforeRecords = records.filter { twoYearAgo <= it.date && it.date < oneYearAgo }
            val thisYearRecords = records.filter { oneYearAgo <= it.date }
            val theThirtyDaysBeforeRecords = thisYearRecords.filter { sixtyDaysAgo <= it.date && it.date < thirtyDaysAgo }
            val thisThirtyDaysRecords = thisYearRecords.filter { thirtyDaysAgo <= it.date }
            val theWeekBeforeRecords = thisThirtyDaysRecords.filter { twoWeekAgo <= it.date && it.date < oneWeekAgo }
            val thisWeekRecords = thisThirtyDaysRecords.filter { oneWeekAgo <= it.date }

            updateWeightTendency(weightUnit, thisWeekRecords, theWeekBeforeRecords, thisThirtyDaysRecords, theThirtyDaysBeforeRecords, thisYearRecords, theYearBeforeRecords)

            updateBMI(weightLatestRecord?.weight ?: 0f, weightUnit)

            val bodyFatRecordsSortedByDate = recordsSortedByDate.filterNot { it.rate == 0f }
            val bodyFatFirstRecord = bodyFatRecordsSortedByDate.firstOrNull()
            val bodyFatLatestRecord = bodyFatRecordsSortedByDate.lastOrNull()
            val bodyFatBestRecord = bodyFatRecordsSortedByDate.minBy { it.rate }
            val goalBodyFat = SharedPreferencesUtil.getFloat(context, SharedPreferencesUtil.Key.GOAL_RATE)

            val isBodyFatDefaultHidden = context.getBoolean(R.bool.is_dashboard_body_fat_default_hidden)
            val showBodyFatProgress = !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_BODY_FAT_PROGRESS, isBodyFatDefaultHidden)
            view?.updateBodyFatProgress(showBodyFatProgress, elapsedDay, bodyFatFirstRecord?.rate, bodyFatBestRecord?.rate, bodyFatLatestRecord?.rate, goalBodyFat)

            updateBodyFatTendency(thisWeekRecords, theWeekBeforeRecords, thisThirtyDaysRecords, theThirtyDaysBeforeRecords, thisYearRecords, theYearBeforeRecords)

            if (context.getBoolean(R.bool.is_dashboard_photo_visible)) {
                updatePhotos(weightUnit, recordsSortedByDate)
            }

            view?.stopRefreshingIfNeeded()
        }
    }

    private fun updateBMI(weight: Float, weightUnit: String) {
        val showBMI = !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_BMI)
        val height = SharedPreferencesUtil.getFloat(context, SharedPreferencesUtil.Key.HEIGHT)
        val showSetHeightButton = height == 0f
        val isLatestWeightBlank = weight == 0f

        if (showSetHeightButton || isLatestWeightBlank) {
            view?.updateBMI(showBMI, showSetHeightButton, null, null, null, weightUnit)

        } else {
            val bmi = (weight / (height / 100.0).pow(2.0)).toFloat()
            val bmiClass = context.getString(BMIClass.getBMIClass(bmi).labelRes)
            val idealWeight = ((height / 100.0).pow(2.0) * 22).toFloat()
            view?.updateBMI(showBMI, showSetHeightButton, bmi, bmiClass, weightScale.convertFromKg(idealWeight), weightUnit)
        }
    }

    private fun updateWeightTendency(
            weightUnit: String,
            thisWeekRecords: List<Record>,
            theWeekBeforeRecords: List<Record>,
            thisThirtyDaysRecords: List<Record>,
            theThirtyDaysBeforeRecords: List<Record>,
            thisYearRecords: List<Record>,
            theYearBeforeRecords: List<Record>
    ) {
        val showBodyFatTendency = !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_WEIGHT_TENDENCY)
        val theWeekBeforeBodyFatList: List<Float> = theWeekBeforeRecords.filter { it.weight != 0f }.map { it.weight }
        val thisWeekBodyFatList: List<Float> = thisWeekRecords.filter { it.weight != 0f }.map { it.weight }
        val thisWeekTendency: Float? = if (theWeekBeforeBodyFatList.isNotEmpty() && thisWeekBodyFatList.isNotEmpty()) (thisWeekBodyFatList.average() - theWeekBeforeBodyFatList.average()).toFloat() else null

        val theThirtyDaysBeforeBodyFatList: List<Float> = theThirtyDaysBeforeRecords.filter { it.weight != 0f }.map { it.weight }
        val thisThirtyDaysBodyFatList: List<Float> = thisThirtyDaysRecords.filter { it.weight != 0f }.map { it.weight }
        val thisThirtyDaysTendency: Float? = if (theThirtyDaysBeforeBodyFatList.isNotEmpty() && thisThirtyDaysBodyFatList.isNotEmpty()) (thisThirtyDaysBodyFatList.average() - theThirtyDaysBeforeBodyFatList.average()).toFloat() else null

        val theYearBeforeBodyFatList: List<Float> = theYearBeforeRecords.filter { it.weight != 0f }.map { it.weight }
        val thisYearBodyFatList: List<Float> = thisYearRecords.filter { it.weight != 0f }.map { it.weight }
        val thisYearTendency: Float? = if (theYearBeforeBodyFatList.isNotEmpty() && thisYearBodyFatList.isNotEmpty()) (thisYearBodyFatList.average() - theYearBeforeBodyFatList.average()).toFloat() else null

        view?.updateWeightTendency(
                showBodyFatTendency,
                weightUnit,
                thisWeekTendency?.let { weightScale.convertFromKg(it) },
                thisThirtyDaysTendency?.let { weightScale.convertFromKg(it) },
                thisYearTendency?.let { weightScale.convertFromKg(it) }
        )
    }

    private fun updateBodyFatTendency(
            thisWeekRecords: List<Record>,
            theWeekBeforeRecords: List<Record>,
            thisThirtyDaysRecords: List<Record>,
            theThirtyDaysBeforeRecords: List<Record>,
            thisYearRecords: List<Record>,
            theYearBeforeRecords: List<Record>
    ) {
        val isBodyFatDefaultHidden = context.getBoolean(R.bool.is_dashboard_body_fat_default_hidden)
        val showWeightTendency = !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_BODY_FAT_TENDENCY, isBodyFatDefaultHidden)
        val theWeekBeforeWeightList: List<Float> = theWeekBeforeRecords.filter { it.rate != 0f }.map { it.rate }
        val thisWeekWeightList: List<Float> = thisWeekRecords.filter { it.rate != 0f }.map { it.rate }
        val thisWeekTendency: Float? = if (theWeekBeforeWeightList.isNotEmpty() && thisWeekWeightList.isNotEmpty()) (thisWeekWeightList.average() - theWeekBeforeWeightList.average()).toFloat() else null

        val theThirtyDaysBeforeWeightList: List<Float> = theThirtyDaysBeforeRecords.filter { it.rate != 0f }.map { it.rate }
        val thisThirtyDaysWeightList: List<Float> = thisThirtyDaysRecords.filter { it.rate != 0f }.map { it.rate }
        val thisThirtyDaysTendency: Float? = if (theThirtyDaysBeforeWeightList.isNotEmpty() && thisThirtyDaysWeightList.isNotEmpty()) (thisThirtyDaysWeightList.average() - theThirtyDaysBeforeWeightList.average()).toFloat() else null

        val theYearBeforeWeightList: List<Float> = theYearBeforeRecords.filter { it.rate != 0f }.map { it.rate }
        val thisYearWeightList: List<Float> = thisYearRecords.filter { it.rate != 0f }.map { it.rate }
        val thisYearTendency: Float? = if (theYearBeforeWeightList.isNotEmpty() && thisYearWeightList.isNotEmpty()) (thisYearWeightList.average() - theYearBeforeWeightList.average()).toFloat() else null

        view?.updateBodyFatTendency(showWeightTendency, thisWeekTendency, thisThirtyDaysTendency, thisYearTendency)
    }

    private fun updatePhotos(weightUnit: String, recordsSortedByDate: List<Record>) {
        val recordsWithWeightSortedByDate = recordsSortedByDate.filter { it.weight != 0f }
        val recordsSortedByWeight = recordsSortedByDate.filter { it.weight != 0f }.sortedBy { it.weight }

        val recordsWithBodyFatSortedByDate = recordsSortedByDate.filter { it.rate != 0f }
        val recordsSortedByBodyFat = recordsSortedByDate.filter { it.rate != 0f }.sortedBy { it.rate }

        val photoSummaryList = listOf(
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_FRONT_PHOTO_SUMMARY_BY_WEIGHT),
                        R.string.front_photo_summary_by_weight_title,
                        weightUnit,
                        recordsWithWeightSortedByDate.firstOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByWeight.firstOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithWeightSortedByDate.lastOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_SIDE_PHOTO_SUMMARY_BY_WEIGHT),
                        R.string.side_photo_summary_by_weight_title,
                        weightUnit,
                        recordsWithWeightSortedByDate.firstOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByWeight.firstOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithWeightSortedByDate.lastOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER1_PHOTO_SUMMARY_BY_WEIGHT),
                        R.string.other1_photo_summary_by_weight_title,
                        weightUnit,
                        recordsWithWeightSortedByDate.firstOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByWeight.firstOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithWeightSortedByDate.lastOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER2_PHOTO_SUMMARY_BY_WEIGHT),
                        R.string.other2_photo_summary_by_weight_title,
                        weightUnit,
                        recordsWithWeightSortedByDate.firstOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByWeight.firstOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithWeightSortedByDate.lastOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER3_PHOTO_SUMMARY_BY_WEIGHT),
                        R.string.other3_photo_summary_by_weight_title,
                        weightUnit,
                        recordsWithWeightSortedByDate.firstOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByWeight.firstOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithWeightSortedByDate.lastOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_FRONT_PHOTO_SUMMARY_BY_BODY_FAT),
                        R.string.front_photo_summary_by_rate_title,
                        weightUnit,
                        recordsWithBodyFatSortedByDate.firstOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByBodyFat.firstOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithBodyFatSortedByDate.lastOrNull { !it.frontImagePath.isNullOrEmpty() }?.let { PhotoData(it.frontImagePath ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_SIDE_PHOTO_SUMMARY_BY_BODY_FAT),
                        R.string.side_photo_summary_by_rate_title,
                        weightUnit,
                        recordsWithBodyFatSortedByDate.firstOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByBodyFat.firstOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithBodyFatSortedByDate.lastOrNull { !it.sideImagePath.isNullOrEmpty() }?.let { PhotoData(it.sideImagePath ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER1_PHOTO_SUMMARY_BY_BODY_FAT),
                        R.string.other1_photo_summary_by_rate_title,
                        weightUnit,
                        recordsWithBodyFatSortedByDate.firstOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByBodyFat.firstOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithBodyFatSortedByDate.lastOrNull { !it.otherImagePath1.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath1 ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER2_PHOTO_SUMMARY_BY_BODY_FAT),
                        R.string.other2_photo_summary_by_rate_title,
                        weightUnit,
                        recordsWithBodyFatSortedByDate.firstOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByBodyFat.firstOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithBodyFatSortedByDate.lastOrNull { !it.otherImagePath2.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath2 ?: "", Date(it.date), it.weight, it.rate) }
                ),
                PhotoSummary(
                        !SharedPreferencesUtil.getBoolean(context, SharedPreferencesUtil.Key.HIDE_OTHER3_PHOTO_SUMMARY_BY_BODY_FAT),
                        R.string.other3_photo_summary_by_rate_title,
                        weightUnit,
                        recordsWithBodyFatSortedByDate.firstOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsSortedByBodyFat.firstOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) },
                        recordsWithBodyFatSortedByDate.lastOrNull { !it.otherImagePath3.isNullOrEmpty() }?.let { PhotoData(it.otherImagePath3 ?: "", Date(it.date), it.weight, it.rate) }
                )
        )

        view?.updatePhotoSummaries(photoSummaryList)
    }
}