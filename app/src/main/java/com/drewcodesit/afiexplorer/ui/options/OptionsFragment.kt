/*
 * // Copyright (c) 2021 Andrew Stephens. All rights reserved.
 * // Licensed under the MIT License. See LICENSE file in the project root for full license information.
 */

package com.drewcodesit.afiexplorer.ui.options

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.FragmentOptionsBinding
import com.drewcodesit.afiexplorer.utils.filter.Filters
import com.drewcodesit.afiexplorer.utils.Config
import com.drewcodesit.afiexplorer.utils.Config.getDBVersion
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet

class OptionsFragment : Fragment() {
    private var _binding : FragmentOptionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences

    private val deviceInfoText =
        "Manufacturer: ${Build.MANUFACTURER}\n" +
                "Model: ${Build.MODEL}\n" +
                "SDK: ${Build.VERSION.SDK_INT}\n" +
                "Board: ${Build.BOARD}\n" +
                "OS: Android ${Build.VERSION.RELEASE}\n" +
                "Arch: ${Build.SUPPORTED_ABIS[0]}\n" +
                "Product: ${Build.PRODUCT}\n"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)

        bottomNav.post {
            val bottomPadding = bottomNav.height
            binding.optionsRecycler.setPadding(
                binding.optionsRecycler.paddingLeft,
                binding.optionsRecycler.paddingTop,
                binding.optionsRecycler.paddingRight,
                bottomPadding + 32  // extra breathing space
            )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptionsBinding.inflate(inflater, container, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val versionHeader = resources.getString(R.string.version_header)
        val databaseVersion = resources.getString(R.string.database_version, requireContext().getDBVersion())

        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        val items = listOf(
            //OptionsItems(0, "Set Default Publication Filter", R.drawable.ic_publications) { setDefaultFilter() },
            OptionsItems(1, "$formattedVersionText\n$databaseVersion", R.drawable.ic_database) {showDeviceInfo()},
            OptionsItems(2, getString(R.string.change_theme), R.drawable.follow_system) { changeTheme() },
            OptionsItems(3, getString(R.string.app_sponsorship), R.drawable.ic_coffee) { getLink(resources.getString(R.string.app_sponsorship_url)) },
            OptionsItems(4, getString(R.string.rating_header), R.drawable.ic_thumbs_up) { getLink(resources.getString(R.string.playstore_link)) },
            OptionsItems(5, getString(R.string.privacy_policy), R.drawable.ic_error) { getLink(resources.getString(R.string.privacy_url)) },
            OptionsItems(6, getString(R.string.licenses), R.drawable.ic_publications) { showOpenSource() },
            OptionsItems(7, getString(R.string.feedback_header), R.drawable.ic_feedback) { feedBack() },
            OptionsItems(8, getString(R.string.social_header), R.drawable.ic_linkedin) { socialMediaConnections() },
            OptionsItems(9, getString(R.string.epubs_notice), R.drawable.ic_change_log) { getLink(resources.getString(R.string.epubs_notice_url)) },
            OptionsItems(10, getString(R.string.resilience_summary), R.drawable.ic_digital_wellbeing) { getLink(resources.getString(R.string.resilience_url)) },
            OptionsItems(11, getString(R.string.doctrine_summary), R.drawable.ic_network_intelligence) { getLink(resources.getString(R.string.af_doctrine_url)) },
        )

        binding.optionsRecycler.adapter = OptionsAdapter(items)
        return binding.root
    }

    private fun getLink(link: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = (link.toUri())
        startActivity(intent)
    }

    private fun feedBack(){
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        val send = Intent(Intent.ACTION_SENDTO)

        // Email subject (App Name and Version Code for troubleshooting)
        val uriText = "mailto:" +
                Uri.encode("drewstephensdesigns@gmail.com") +
                "?subject=" + Uri.encode("App Feedback: ") +
                resources.getString(R.string.app_name) + " " + formattedVersionText
        val uri = uriText.toUri()
        send.data = uri

        // Displays apps that are able to handle email
        startActivity(Intent.createChooser(send, "Send feedback..."))
    }


    private fun showOpenSource(){
        startActivity(Intent(requireContext(), OssLicensesMenuActivity::class.java))
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
                intent.data = (href.toUri())
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
                        sharedPreferences.edit {
                            putInt(
                                getString(R.string.pref_key_mode_night),
                                AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }

                    }
                    1 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        sharedPreferences.edit {
                            putInt(
                                getString(R.string.pref_key_mode_night),
                                AppCompatDelegate.MODE_NIGHT_YES
                            )
                        }

                    }
                    2 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        sharedPreferences.edit {
                            putInt(
                                getString(R.string.pref_key_mode_night),
                                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            )
                        }
                    }
                }
            }
        }
    }

    private fun socialMediaConnections(){
        OptionSheet().show(requireContext()){
            style(SheetStyle.BOTTOM_SHEET)
            displayToolbar(true)
            title("Let's Connect!")
            displayMode(DisplayMode.LIST)
            with(
                Option(R.drawable.ic_linkedin, "LinkedIn"),
                Option(R.drawable.ic_github, "Github"),
                Option(R.drawable.ic_camera, "Instagram"),
                Option(R.drawable.ic_snoo, "r/AirForce"),
                Option(R.drawable.ic_change_log, "View App Change Log & Source")
            )
            onPositive { index: Int, _: Option ->
                when(index){
                    0 -> {getLink(resources.getString(R.string.developer_linkedin_url))}
                    1 -> {getLink(resources.getString(R.string.developer_github_url))}
                    2 -> {getLink(resources.getString(R.string.developer_instagram_url))}
                    3 -> {getLink(resources.getString(R.string.developer_reddit_url))}
                    4 -> {getLink(resources.getString(R.string.change_log_url))}
                }
            }
        }
    }

    // Displays users device info
    private fun showDeviceInfo(){
        InfoSheet().show(requireContext()){
            style(SheetStyle.BOTTOM_SHEET)
            title("Device Information")
            content(deviceInfoText)
            onPositive("Copy to clipboard"){
                Config.save(requireContext(), deviceInfoText)
            }
        }
    }

    // Allow user to set a default filter
    private fun setDefaultFilter() {
        val allFilters = Filters.externalOrg + Filters.organizations + Filters.commands + Filters.bases
        val displayOptions = mutableListOf("None (Show All)") + allFilters.map { it.displayName }

        OptionSheet().show(requireContext()) {
            title("Select Default Filter")
            style(SheetStyle.BOTTOM_SHEET)
            displayToolbar(true)
            displayMode(DisplayMode.LIST)

            // FIX: Convert the mapped List explicitly into a MutableList
            val optionsList = displayOptions.map {
                Option(R.drawable.ic_publications, it)
            }.toMutableList()

            with(optionsList)

            onPositive { index: Int, _: Option ->
                val savedValue = if (index == 0) "" else allFilters[index - 1].filterValue

                sharedPreferences.edit {
                    //putString(getString(R.string.pref_key_default_filter), savedValue)
                    Log.e("SHARED PREFS FAKE", "Saved Value is: == $savedValue")
                }

                Snackbar.make(binding.root, "Default filter updated!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}