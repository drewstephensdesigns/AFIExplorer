package com.drewcodesit.afiexplorer.ui.options

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.room.RoomDatabase
import com.drewcodesit.afiexplorer.MainActivity
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FragmentOptionsBinding
import com.drewcodesit.afiexplorer.utils.Config
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet

class OptionsFragment : Fragment() {

    private var _binding : FragmentOptionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    private val deviceInfoText = "Manufacturer: ${Build.MANUFACTURER}\n" +
            "Model: ${Build.MODEL}\n" +
            "SDK: ${Build.VERSION.SDK_INT}\n" +
            "Board: ${Build.BOARD}\n" +
            "OS: Android ${Build.VERSION.RELEASE}\n" +
            "Arch: ${Build.SUPPORTED_ABIS[0]}\n" +
            "Product: ${Build.PRODUCT}"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionsBinding.inflate(inflater, container, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Gets version info from Gradle/Manifest
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        binding.textBuildVersion.text = formattedVersionText

        // displays back arrow
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        /**
         * Change App Theme
         * Buy a Coffee
         * Rate App
         * Privacy Policy
         */
        binding.theme.setOnClickListener { changeTheme() }
        setupCard(binding.donate, resources.getString(R.string.app_sponsorship_url))
        setupCard(binding.rate, resources.getString(R.string.playstore_link))
        setupCard(binding.privacy, resources.getString(R.string.privacy_url))

        // Displays Google Play license builder
        binding.openSource.setOnClickListener {
            startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.licenses))
        }

        // App Feedback
        binding.feedback.setOnClickListener { feedBack() }

        // Social Media Links
        binding.connect.setOnClickListener {
            OptionSheet().show(requireContext()){
                style(SheetStyle.BOTTOM_SHEET)
                displayToolbar(true)
                title("Follow Me on these Platforms")
                displayMode(DisplayMode.LIST)
                with(
                    Option(R.drawable.ic_linkedin, "LinkedIn"),
                    Option(R.drawable.ic_github, "Github"),
                    Option(R.drawable.ic_camera, "Instagram"),
                    Option(R.drawable.ic_snoo, "r/AirForce"),
                    Option(R.drawable.ic_change_log, "Change Log")
                )
                onPositive { index: Int, _: Option ->
                    when(index){
                        0 -> {getLink(resources.getString(R.string.developer_linkedin_url))}
                        1 -> {getLink(resources.getString(R.string.developer_github_url))}
                        2 -> {getLink(resources.getString(R.string.developer_instagram_url))}
                        3 -> {getLink(resources.getString(R.string.change_log_url))}
                        4 -> {getLink(resources.getString(R.string.reddit))}
                    }
                }
            }
        }

        // E-Pubs Change Announcements
        setupCard(binding.epubsChanges, resources.getString(R.string.epubs_notice_url))
    }

    private fun getLink(link: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = (Uri.parse(link))
        startActivity(intent)
    }

    private fun feedBack(){
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        val send = Intent(Intent.ACTION_SENDTO)

        // Email subject (App Name and Version Code for troubleshooting)
        val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                "?subject=" + Uri.encode("App Feedback: ") + resources.getString(R.string.app_name) + " " + formattedVersionText
        val uri = Uri.parse(uriText)
        send.data = uri

        // Displays apps that are able to handle email
        startActivity(Intent.createChooser(send, "Send feedback..."))
    }

    private fun setupCard(card: MaterialCardView, link: String) {
        card.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = (Uri.parse(link))
            startActivity(intent)
        }
        card.setOnLongClickListener {
            onLongClick(link)
            true
        }
    }

    // Long Press to Copy href links to clipboard
    private fun onLongClick(href: String) {
        // copy the link to the clipboard
        Config.save(requireContext(), href)
        // show the snackBar with open action
        Snackbar.make(
            binding.root,
            R.string.copied_to_clipboard,
            Snackbar.LENGTH_LONG
        )
            .setAction(R.string.open_copied) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = (Uri.parse(href))
                startActivity(intent)
            }
            .setAnimationMode(Snackbar.ANIMATION_MODE_FADE)
            .show()
    }

    private fun changeTheme(){
        OptionSheet().show(requireContext()){
            title("Set Theme")
            displayToolbar(true)
            style(SheetStyle.DIALOG)
            displayMode(DisplayMode.LIST)
            with(
                Option(R.drawable.theme_switch, "Light"),
                Option(R.drawable.outline_nightlight, "Dark"),
                Option(R.drawable.follow_system, "Follow System")
            )
            onPositive { index: Int, _: Option ->
                when(index){
                    0 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_NO
                        ).apply()

                    }
                    1 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_YES
                        ).apply()

                    }
                    2 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        ).apply()
                    }
                }
            }
        }
    }

    // Displays users device info
    private fun showDeviceInfo(){
        InfoSheet().show(requireContext()){
            style(SheetStyle.DIALOG)
            title("Device Info")
            content(deviceInfoText)
            onPositive("Ok")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}