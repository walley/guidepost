package org.walley.guidepost

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class f_settings : PreferenceFragmentCompat() {

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.root_preferences, rootKey)
  }
}