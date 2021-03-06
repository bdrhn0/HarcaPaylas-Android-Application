package com.bedirhandag.harcapaylas.ui.fragment.addreport

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.bedirhandag.harcapaylas.R
import com.bedirhandag.harcapaylas.databinding.FragmentAddReportBinding
import com.bedirhandag.harcapaylas.model.ReportModel
import com.bedirhandag.harcapaylas.util.*
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_GROUPKEY
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_GROUPS
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_GROUP_MEMBERS
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_REPORTS
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_USERNAME
import com.bedirhandag.harcapaylas.util.FirebaseKeys.KEY_USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddReportFragment : Fragment() {

    private lateinit var viewModel: AddReportViewModel
    private lateinit var viewBinding: FragmentAddReportBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentAddReportBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        initFirebase()
        getUsername()
        initToolbar()
        initListener()
        getArgParams()
        getReports()
    }

    private fun getReports() {
        viewModel.ref.child(KEY_GROUPS)
            .child(viewModel.groupKey)
            .child(KEY_REPORTS)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.value?.let {
                        viewModel.reportList.value = (it as ArrayList<ReportModel>)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun getArgParams() {
        arguments?.getString(KEY_GROUPKEY)?.let {
            viewModel.groupKey = it
        }
    }

    private fun initFirebase() {
        viewModel.ref = FirebaseDatabase.getInstance().reference
        FirebaseAuth.getInstance().currentUser?.uid?.let { viewModel.userUID = it }
    }

    private fun getUsername() {
        viewModel.apply {
            ref.child(KEY_USERS)
                .child(userUID)
                .child(KEY_USERNAME)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.value?.let {
                            username = it.toString()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun initListener() {
        viewBinding.apply {
            btnAddReport.setOnClickListener { addReportOperation() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backButtonOperation()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun backButtonOperation() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this@AddReportFragment)
            .commit()
    }

    private fun addReportOperation() {
        viewBinding.apply {
            when {
                edtDesc.text.toString().isEmpty() || edtPrice.text.toString().isEmpty() -> {
                    showAlert(
                        context = requireActivity(),
                        title = getString(R.string.add_report_warning_title),
                        msg = getString(R.string.add_report_warning_message),
                        iconResId = R.drawable.ic_warning
                    )
                }
                else -> {
                    val reportModel = ReportModel(
                        viewModel.userUID,
                        viewModel.username,
                        edtDesc.text.toString(),
                        edtPrice.text.toString()
                    )

                    viewModel.reportList.value?.add(reportModel)

                    viewModel.apply {
                        ref.child(KEY_GROUPS)
                            .child(groupKey)
                            .child(KEY_REPORTS)
                            .setValue(viewModel.reportList.value)
                    }
                    updateGroupMemberTotalPrice()
                    showAlert(
                        context = requireActivity(),
                        title = getString(R.string.add_report_success_title),
                        msg = getString(R.string.add_report_success_message),
                        iconResId = R.drawable.ic_tick
                    ) {
                        addReportComplete?.isCompleted(reportModel)
                        backButtonOperation()
                    }
                }
            }
        }
    }

    private fun updateGroupMemberTotalPrice() {
        viewModel.ref.child(KEY_GROUPS)
            .child(viewModel.groupKey)
            .child(KEY_GROUP_MEMBERS)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    (snapshot.value as ArrayList<HashMap<String, String>>?)?.convertToTransactionDetailList()?.apply {
                            forEach {
                                if (it.userId == viewModel.userUID) {
                                    val totalPrice = (it.price?.toIntOrNull()?:0) + (viewBinding.edtPrice.text.toString().toIntOrNull()?:0)
                                    it.price = totalPrice.toString()
                                }
                            }
                        }.also { _newList ->
                            viewModel.ref.child(KEY_GROUPS)
                                .child(viewModel.groupKey)
                                .child(KEY_GROUP_MEMBERS)
                                .setValue(_newList)
                        }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun initToolbar() {
        viewBinding.addReportAppBar.apply {
            pageTitle.text = getString(R.string.placeholder_add_report)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this).get(AddReportViewModel::class.java)
    }

    companion object {
        var addReportComplete: AddReportCompleteListener? = null
        fun newInstance(
            groupKey: String,
            AddReportCompleteListener: AddReportCompleteListener
        ): AddReportFragment {
            addReportComplete = AddReportCompleteListener
            val addReportFragment = AddReportFragment()
            addReportFragment.arguments = bundleOf(KEY_GROUPKEY to groupKey)
            return addReportFragment
        }
    }
}