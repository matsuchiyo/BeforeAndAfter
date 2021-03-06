package org.macho.beforeandafter.record

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.record_frag.*
import org.macho.beforeandafter.R
import org.macho.beforeandafter.databinding.ListItemRecordBinding
import org.macho.beforeandafter.shared.GlideApp
import org.macho.beforeandafter.shared.data.record.Record
import org.macho.beforeandafter.shared.di.ActivityScoped
import org.macho.beforeandafter.shared.util.AdUtil
import org.macho.beforeandafter.shared.util.WeightScale
import java.io.File
import java.text.SimpleDateFormat
import javax.inject.Inject
import kotlin.math.roundToInt

@ActivityScoped
class RecordFragment @Inject constructor() : DaggerFragment(), RecordContract.View {
    @Inject
    override lateinit var presenter: RecordContract.Presenter

    private lateinit var recordAdapter: RecordAdapter

    private lateinit var weightScale: WeightScale

    // MARK: - Lifecycle
    override fun onCreateView(layoutInflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return layoutInflater.inflate(R.layout.record_frag, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false)
        fab.setOnClickListener { _ ->
            presenter.openAddRecord()
        }

        AdUtil.initializeMobileAds(requireContext())
        AdUtil.loadBannerAd(adView, requireContext())
        adLayout.visibility = if (AdUtil.isBannerAdHidden(requireContext())) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        presenter.takeView(this)

        weightScale = WeightScale(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.dropView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // do nothing
    }


    override fun showItems(items: List<Record>) {
        val recordItems = convertToRecordItemList(items)
        recordAdapter = RecordAdapter(requireContext(), recordItems, weightScale)
        listView.adapter = recordAdapter
    }

    private fun convertToRecordItemList(records: List<Record>): List<RecordItem> {
        var recordItems = mutableListOf<RecordItem>()
        val yearFormatter = SimpleDateFormat("yyyy")
        val dateFormatter = SimpleDateFormat("MM/dd")
        val timeFormatter = SimpleDateFormat("kk:mm")
        for ((index, record) in records.withIndex()) {
            val beforeRecord: Record? = if (index + 1 < records.size) records[index + 1] else null
            val weightDiff: Float? = beforeRecord?.let { if (beforeRecord.weight > 0.0f) record.weight - beforeRecord.weight else null }
            val rateDiff: Float? = beforeRecord?.let { if (beforeRecord.rate > 0.0f) record.rate - beforeRecord.rate else null }
            recordItems.add(RecordItem(
                    record.date,
                    yearFormatter.format(record.date),
                    dateFormatter.format(record.date),
                    timeFormatter.format(record.date),
                    record.weight.let { weightScale.convertFromKg(it) },
                    weightDiff,
                    record.rate,
                    rateDiff,
                    record.frontImagePath,
                    record.sideImagePath,
                    record.memo
            ))
        }
        return recordItems
    }

    override fun showAddRecordUI() {
//        val bundle = Bundle()
//        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "myId")
//        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "add")
//        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
//        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        val title = getString(R.string.action_bar_title_record_detail_new)
        val action = RecordFragmentDirections.actionRecordFragmentToEditAddRecordFragment(0L, title)
        findNavController().navigate(action)
    }

    override fun showEditRecordUI(date: Long) {
        val title = getString(R.string.action_bar_title_record_detail_edit)
        val action = RecordFragmentDirections.actionRecordFragmentToEditAddRecordFragment(date, title)
        findNavController().navigate(action)
    }

    override fun hideEmptyView() {
        emptyView.visibility = View.GONE
        listView.visibility = View.VISIBLE
    }

    override fun showEmptyView() {
        emptyView.visibility = View.VISIBLE
        listView.visibility = View.GONE
    }

    inner class RecordAdapter(val context: Context, val records: List<RecordItem>, val weightScale: WeightScale)
        : RecyclerView.Adapter<RecordAdapter.RecordItemViewHolder>() {
        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

        private var lastClickTime = 0L

        override fun getItemCount(): Int {
            return records.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordItemViewHolder {
            val view = layoutInflater.inflate(R.layout.list_item_record, parent, false)
            return RecordItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecordItemViewHolder, position: Int) {
            holder.binding.item = records.get(position)
            holder.binding.weightUnit = weightScale.weightUnitText
            holder.binding.executePendingBindings()
        }

        inner class RecordItemViewHolder(view: View): RecyclerView.ViewHolder(view) {
            val binding = ListItemRecordBinding.bind(view)

            init {
                view.setOnClickListener {_ ->
                    // NOTE: workaround crashlytics: navigation destination org.macho.beforeandafter:id/action_recordFragment_to_editAddRecordFragment is unknown to this NavController
                    if (System.currentTimeMillis() - lastClickTime < 1000) {
                        return@setOnClickListener
                    }

                    lastClickTime = System.currentTimeMillis()

                    val date = this@RecordItemViewHolder.binding.item?.date ?: return@setOnClickListener
                    presenter.openEditRecord(date)
                }
            }
        }
    }
}

@BindingAdapter("floatValue")
fun formatFloat(view: TextView, floatValue: Float) {
    val roundedValue = (floatValue * 10).roundToInt() / 10f
    val text = String.format("%.1f", roundedValue)
    view.setText(text, null)
}

@BindingAdapter("floatValueWithSign")
fun formatFloatWithSign(view: TextView, floatValue: Float) {
    val roundedValue = (floatValue * 10).roundToInt() / 10f
    val roundedValueText = String.format("%.1f", roundedValue)
    if (floatValue == 0f) {
        view.setText("±$roundedValueText", null)
        view.setTextColor(Color.GRAY)
    } else if (floatValue < 0f) {
        view.setText("$roundedValueText", null)
        view.setTextColor(Color.GREEN)
    } else if (floatValue > 0f) {
        view.setText("+$roundedValueText", null)
        view.setTextColor(Color.RED)
    }
}

@BindingAdapter("imageFilePath")
fun loadImage(view: ImageView, path: String?) {
    if (path == null) {
        view.setImageResource(android.R.color.transparent)
        return
    }

    val file = File(view.context.filesDir, path)
    if (!(file.exists() && file.isFile)) {
        view.setImageResource(android.R.color.transparent)
        return
    }

    GlideApp.with(view.context)
            .load(Uri.fromFile(file))
            .sizeMultiplier(.4f)
            .thumbnail(.1f)
            .error(ColorDrawable(Color.LTGRAY))
            .into(view)
}