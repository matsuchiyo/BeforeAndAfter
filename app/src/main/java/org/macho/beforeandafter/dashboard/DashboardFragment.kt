package org.macho.beforeandafter.dashboard

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.formats.MediaView
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.formats.UnifiedNativeAd
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.dashboard_frag.*
import org.macho.beforeandafter.R
import org.macho.beforeandafter.dashboard.view.*
import org.macho.beforeandafter.shared.di.FragmentScoped
import org.macho.beforeandafter.shared.extensions.getBoolean
import org.macho.beforeandafter.shared.util.AdUtil
import org.macho.beforeandafter.shared.util.LogUtil
import org.macho.beforeandafter.shared.view.commondialog.CommonDialog
import javax.inject.Inject
import kotlin.math.max

@FragmentScoped
class DashboardFragment @Inject constructor(): DaggerFragment(), DashboardContract.View {

    @Inject
    override lateinit var presenter: DashboardContract.Presenter

    @Inject
    lateinit var dialog: CommonDialog

    private var currentNativeAd: UnifiedNativeAd? = null

    // MARK: - Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dashboard_frag, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        swipeRefreshLayout.setOnRefreshListener {
            presenter.reloadDashboard()
        }

        fab.setOnClickListener {
            presentAddRecord()
        }

        setHasOptionsMenu(true)

        refreshAd()

        AdUtil.initializeMobileAds(requireContext())
        AdUtil.loadBannerAd(adView, requireContext())
        adLayout.visibility = if (AdUtil.isBannerAdHidden(requireContext())) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        presenter.takeView(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentNativeAd?.destroy()
        presenter.dropView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.setting -> {
                val action = DashboardFragmentDirections.actionDashboardFragmentToDashboardSettingFragment2()
                findNavController().navigate(action)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // MARK: - DashboardContract.View
    override fun toggleEmptyView(show: Boolean) {
        emptyView.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    override fun updateWeightProgress(show: Boolean, weightUnit: String, elapsedDay: Int, firstWeight: Float?, bestWeight: Float?, latestWeight: Float?, goalWeight: Float?) {
        if (!show) {
            linearLayout.findViewById<CardView>(R.id.weight_progress_card_id)?.let {
                linearLayout.removeView(it)
            }
            return
        }

        val weightProgressView = linearLayout.findViewById<DashboardProgressView>(R.id.weight_progress_view_id) ?: DashboardProgressView(requireContext()).also {
            addCardView(it, R.id.weight_progress_view_id, R.id.weight_progress_card_id)
        }

        weightProgressView.update(getString(R.string.progress_title), ContextCompat.getColor(requireContext(), R.color.colorPrimaryLight), weightUnit, R.drawable.background_current_weight_label, elapsedDay, firstWeight, bestWeight, latestWeight, goalWeight, goalWeight == 0f, object: DashboardProgressViewListener {
            override fun onSetGoalButtonClicked() {
                val action = DashboardFragmentDirections.actionDashboardFragmentToEditGoalFragment2()
                findNavController().navigate(action)
            }

            override fun onElapsedDayHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.elapsed_days_help_message), getString(R.string.ok))
            }

            override fun onAchieveExpectHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.archive_expect_days_help_message), getString(R.string.ok))
            }
        })
    }

    override fun updateWeightTendency(show: Boolean, weightUnit: String, oneWeekTendency: Float?, thirtyDaysTendency: Float?, oneYearTendency: Float?) {
        if (!show) {
            linearLayout.findViewById<CardView>(R.id.weight_tendency_card_id)?.let {
                linearLayout.removeView(it)
            }
            return
        }

        val weightTendencyView = linearLayout.findViewById<DashboardTendencyView>(R.id.weight_tendency_view_id) ?: DashboardTendencyView(requireContext()).also {
            addCardView(it, R.id.weight_tendency_view_id, R.id.weight_tendency_card_id)
        }

        weightTendencyView.update(getString(R.string.weight_tendency_title), weightUnit, oneWeekTendency, thirtyDaysTendency, oneYearTendency, object: DashboardTendencyView.DashboardTendencyViewListener {
            override fun onOneWeekTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.one_week_tendency_help), getString(R.string.ok))
            }

            override fun onThirtyDaysTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.thirty_days_tendency_help), getString(R.string.ok))
            }

            override fun onOneYearTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.one_year_tendency_help), getString(R.string.ok))
            }
        })
    }

    override fun updateBMI(show: Boolean, showSetHeightButton: Boolean, bmi: Float?, bmiClass: String?, idealWeight: Float?, weightUnit: String) {
        if (!show) {
            linearLayout.findViewById<CardView>(R.id.bmi_card_id)?.let {
                linearLayout.removeView(it)
            }
            return
        }

        val bmiView = linearLayout.findViewById<DashboardBMIView>(R.id.bmi_view_id) ?: DashboardBMIView(requireContext()).also {
            addCardView(it, R.id.bmi_view_id, R.id.bmi_card_id)
        }

        bmiView.update(showSetHeightButton, bmi, bmiClass, idealWeight, weightUnit) {
            val action = DashboardFragmentDirections.actionDashboardFragmentToEditHeightFragment()
            findNavController().navigate(action)
        }
    }

    override fun updateBodyFatProgress(show: Boolean, elapsedDay: Int, firstBodyFat: Float?, bestBodyFat: Float?, latestBodyFat: Float?, goalBodyFat: Float?) {
        if (!show) {
            linearLayout.findViewById<CardView>(R.id.body_fat_progress_card_id)?.let {
                linearLayout.removeView(it)
            }
            return
        }

        val bodyFatProgressView = linearLayout.findViewById<DashboardProgressView>(R.id.body_fat_progress_view_id) ?: DashboardProgressView(requireContext()).also {
            addCardView(it, R.id.body_fat_progress_view_id, R.id.body_fat_progress_card_id)
        }

        bodyFatProgressView.update(getString(R.string.body_fat_progress_title), ContextCompat.getColor(requireContext(), R.color.colorAccent), "%%", R.drawable.background_current_body_fat_label, elapsedDay, firstBodyFat, bestBodyFat, latestBodyFat, goalBodyFat, goalBodyFat == 0f, object: DashboardProgressViewListener {
            override fun onSetGoalButtonClicked() {
                val action = DashboardFragmentDirections.actionDashboardFragmentToEditGoalFragment2()
                findNavController().navigate(action)
            }

            override fun onElapsedDayHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.elapsed_days_help_message), getString(R.string.ok))
            }

            override fun onAchieveExpectHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.archive_expect_days_help_message), getString(R.string.ok))
            }
        })
    }

    override fun updateBodyFatTendency(show: Boolean, oneWeekTendency: Float?, thirtyDaysTendency: Float?, oneYearTendency: Float?) {
        if (!show) {
            linearLayout.findViewById<CardView>(R.id.body_fat_tendency_card_id)?.let {
                linearLayout.removeView(it)
            }
            return
        }

        val bodyFatTendencyView = linearLayout.findViewById<DashboardTendencyView>(R.id.body_fat_tendency_view_id) ?: DashboardTendencyView(requireContext()).also {
            addCardView(it, R.id.body_fat_tendency_view_id, R.id.body_fat_tendency_card_id)
        }

        bodyFatTendencyView.update(getString(R.string.body_fat_tendency_title), "%%", oneWeekTendency, thirtyDaysTendency, oneYearTendency, object: DashboardTendencyView.DashboardTendencyViewListener {
            override fun onOneWeekTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.one_week_tendency_help), getString(R.string.ok))
            }

            override fun onThirtyDaysTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.thirty_days_tendency_help), getString(R.string.ok))
            }

            override fun onOneYearTendencyHelpButtonClicked() {
                dialog.show(parentFragmentManager, 0, getString(R.string.one_year_tendency_help), getString(R.string.ok))
            }
        })
    }

    override fun updatePhotoSummaries(photoSummaries: List<PhotoSummary>) {
        val idTupleList: List<Pair<Int, Int>> = listOf(
            Pair(R.id.front_photo_summary_by_weight_card, R.id.front_photo_summary_by_weight_view),
            Pair(R.id.side_photo_summary_by_weight_card, R.id.side_photo_summary_by_weight_view),
            Pair(R.id.other1_photo_summary_by_weight_card, R.id.other1_photo_summary_by_weight_view),
            Pair(R.id.other2_photo_summary_by_weight_card, R.id.other2_photo_summary_by_weight_view),
            Pair(R.id.other3_photo_summary_by_weight_card, R.id.other3_photo_summary_by_weight_view),
            Pair(R.id.front_photo_summary_by_rate_card, R.id.front_photo_summary_by_rate_view),
            Pair(R.id.side_photo_summary_by_rate_card, R.id.side_photo_summary_by_rate_view),
            Pair(R.id.other1_photo_summary_by_rate_card, R.id.other1_photo_summary_by_rate_view),
            Pair(R.id.other2_photo_summary_by_rate_card, R.id.other2_photo_summary_by_rate_view),
            Pair(R.id.other3_photo_summary_by_rate_card, R.id.other3_photo_summary_by_rate_view)
        )

        for ((i, photoSummary) in photoSummaries.withIndex()) {
            val cardId = idTupleList[i].first
            val viewId = idTupleList[i].second

            if (!photoSummary.isVisible) {
                linearLayout.findViewById<CardView>(cardId)?.let {
                    linearLayout.removeView(it)
                }
                return
            }

            val photoSummaryView = linearLayout.findViewById<DashboardPhotoSummaryView>(viewId) ?: DashboardPhotoSummaryView(requireContext()).also {
                addCardView(it, viewId, cardId)
            }

            photoSummaryView.update(getString(photoSummary.titleStringResource), photoSummary.weightUnit, photoSummary.firstPhotoData, photoSummary.bestPhotoData, photoSummary.latestPhotoData)
        }
    }


    override fun stopRefreshingIfNeeded() {
        swipeRefreshLayout.isRefreshing = false
    }

    // MARK: - Private

    private fun addCardView(cardContentView: View, contentViewId: Int, cardId: Int) {
        cardContentView.id = contentViewId
        val cardView = CardView(requireContext())
        cardView.id = cardId
        cardView.addView(cardContentView, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val childCount = linearLayout.childCount
        linearLayout.addView(cardView, max(0, childCount - 1), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
            val marginInPx = convertDpToPx(requireContext(), 12)
            it.setMargins(marginInPx, marginInPx, marginInPx, 0)
        })
    }

    private fun convertDpToPx(context: Context, dp: Int): Int {
        val d: Float = context.resources.displayMetrics.density
        return (dp * d + 0.5).toInt()
    }

    private fun presentAddRecord() {
        val title = getString(R.string.action_bar_title_record_detail_new)
        val action = DashboardFragmentDirections.actionDashboardFragmentToEditAddRecordFragment2(0L, title)
        findNavController().navigate(action)
    }

    private fun refreshAd() {
        if (requireContext().getBoolean(R.bool.hide_native_ad)) {
            return
        }

        val builder = AdLoader.Builder(requireContext(), getString(R.string.admob_unit_id_native))

        builder.forUnifiedNativeAd { unifiedNativeAd ->
            if (this.view == null) {
                unifiedNativeAd.destroy()
                return@forUnifiedNativeAd
            }

            currentNativeAd?.destroy()
            currentNativeAd = unifiedNativeAd
            val adView = layoutInflater
                    .inflate(R.layout.ad_unified, null) as UnifiedNativeAdView
            populateUnifiedNativeAdView(unifiedNativeAd, adView)
            ad_frame.removeAllViews()
            ad_frame.addView(adView)
        }

        val adOptions = NativeAdOptions.Builder().build()

        builder.withNativeAdOptions(adOptions)

        val adLoader = builder.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                LogUtil.e(this@DashboardFragment, "Failed to load ad. code: $p0")
            }
        }).build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateUnifiedNativeAdView(nativeAd: UnifiedNativeAd, adView: UnifiedNativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        adView.mediaView.setMediaContent(nativeAd.mediaContent)

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView.visibility = View.INVISIBLE
        } else {
            adView.bodyView.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView.visibility = View.INVISIBLE
        } else {
            adView.callToActionView.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                    nativeAd.icon.drawable
            )
            adView.iconView.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            adView.priceView.visibility = View.INVISIBLE
        } else {
            adView.priceView.visibility = View.VISIBLE
            (adView.priceView as TextView).text = nativeAd.price
        }

        if (nativeAd.store == null) {
            adView.storeView.visibility = View.INVISIBLE
        } else {
            adView.storeView.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            adView.starRatingView.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}