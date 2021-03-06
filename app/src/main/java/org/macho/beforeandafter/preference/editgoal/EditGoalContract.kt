package org.macho.beforeandafter.preference.editgoal

import org.macho.beforeandafter.shared.BaseContract
import java.util.*

interface EditGoalContract {
    interface View: BaseContract.View<Presenter> {
        fun setWeightGoalText(weightGoalText: String?, weightUnit: String)
        fun setRateGoalText(RateGoalText: String?)
        fun updateStartTime(isCustom: Boolean, startTime: Date)
        fun finish()
    }

    interface Presenter: BaseContract.Presenter<View> {
        fun saveGoal(weightGoalText: String, rateGoalText: String, isCustom: Boolean, startTime: Date)
        fun back()
    }
}