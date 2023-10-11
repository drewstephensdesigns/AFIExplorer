package com.drewcodesit.afiexplorer.view


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.SettingsActivityBinding
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity.setActivityTitle
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation
import es.dmoral.toasty.Toasty


class SettingsActivity : AppCompatActivity() {

    private lateinit var _binding: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kotlin View Binding
        _binding = SettingsActivityBinding.inflate(layoutInflater)

        setContentView(_binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        setSupportActionBar(_binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = resources.getString(R.string.action_support)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // Open Source Licenses
            findPreference<Preference>("licenses")?.setOnPreferenceClickListener {
                context?.let {
                    Intent(it, OssLicensesMenuActivity::class.java).apply {
                        setActivityTitle(getString(R.string.licenses))
                        startActivity(this)
                    }
                }
                true
            }

            // Copies version code
            findPreference<Preference>("versionInfo")?.setOnPreferenceClickListener {
                context?.let {
                    val clipboard =
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    val clip = ClipData.newPlainText(
                        "Copied Version!",
                        "Build Version " + getString(R.string.versionName)
                    )
                    clipboard.setPrimaryClip(clip)

                    // Only show a toast for Android 12 and lower.
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        Toasty.info(requireContext(), "Copied Build Version!", Toast.LENGTH_SHORT)
                            .show()
                }
                true
            }

            // Sort of an Easter Egg
            findPreference<Preference>("acknowledge")?.setOnPreferenceClickListener {
                context?.let {
                    InfoSheet().show(requireContext()) {
                        style(SheetStyle.BOTTOM_SHEET)
                        title("Special Thanks and Consideration")
                        content(R.string.reddit_url)
                        onPositive("View r/AirForce") {
                            val intent = Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.reddit.com/r/AirForce")
                            )
                            startActivity(intent)
                        }

                        withCoverLottieAnimation(LottieAnimation {
                            setupAnimation {
                                setAnimation(R.raw.thank_you_anim)
                            }
                        })
                    }
                }
                true
            }

            // Email Pref
            findPreference<Preference>("feedPref")?.setOnPreferenceClickListener {
                val text = "Troubleshooting\n" +
                        "Manufacturer: ${Build.MANUFACTURER}\n" +
                        "Model: ${Build.MODEL}\n" +
                        "SDK: ${Build.VERSION.SDK_INT}\n" +
                        "OS: Android ${Build.VERSION.RELEASE}\n" +
                        "*************"

                context.let {
                    val send = Intent(Intent.ACTION_SENDTO)
                    val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                            "?subject=" + Uri.encode("App Feedback") +
                            "&body=" + Uri.encode(text)
                    val uri = Uri.parse(uriText)

                    send.data = uri
                    startActivity(Intent.createChooser(send, "Send mail..."))
                }
                true
            }
        }
    }
}
