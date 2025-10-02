package org.walley.guidepost;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.config.IConfigurationProvider;
import org.walley.guidepost.databinding.FragmentHomeBinding;

import static org.osmdroid.tileprovider.util.StorageUtils.getStorage;

public class HomeFragment extends Fragment
{

  private FragmentHomeBinding binding;

  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState
                          )
  {

    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                                       .build());

    binding = FragmentHomeBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

    IConfigurationProvider provider = Configuration.getInstance();
    provider.setUserAgentValue(BuildConfig.APPLICATION_ID);
    provider.setOsmdroidBasePath(getStorage());
    provider.setOsmdroidTileCache(getStorage());
    return root;
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    binding = null;
  }
}
