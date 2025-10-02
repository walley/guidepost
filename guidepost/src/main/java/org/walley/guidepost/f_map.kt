package org.walley.guidepost

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.walley.guidepost.databinding.FragmentHomeBinding

class f_map : Fragment() {

  private var binding: FragmentHomeBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    StrictMode.setThreadPolicy(
      ThreadPolicy.Builder()
        .build()
    )
    binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding!!.getRoot()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}
