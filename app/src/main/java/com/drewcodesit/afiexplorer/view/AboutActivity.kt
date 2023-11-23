package com.drewcodesit.afiexplorer.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.drewcodesit.afiexplorer.R
import com.drewcodesit.afiexplorer.databinding.ActivityAboutBinding
import com.drewcodesit.afiexplorer.utils.Config
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.info.InfoSheet
import com.maxkeppeler.sheets.lottie.LottieAnimation
import com.maxkeppeler.sheets.lottie.withCoverLottieAnimation

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    private val deviceInfoText = "Manufacturer: ${Build.MANUFACTURER}\n" +
            "Model: ${Build.MODEL}\n" +
            "SDK: ${Build.VERSION.SDK_INT}\n" +
            "Board: ${Build.BOARD}\n" +
            "OS: Android ${Build.VERSION.RELEASE}\n" +
            "Arch: ${Build.SUPPORTED_ABIS[0]}\n" +
            "Product: ${Build.PRODUCT}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Gets version info from Gradle/Manifest
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        val formattedVersionText = getString(R.string.version_text, versionHeader, versionName)

        // Build Version of App
        binding.appVersionTV.text = formattedVersionText

        // Sponsorship
        setupCard(binding.donate, resources.getString(R.string.app_sponsorship_url))

        // App Rating
        setupCard(binding.rate, resources.getString(R.string.playstore_link))

        // Social Media Links
        setupCard(binding.linkedin, resources.getString(R.string.developer_linkedin_url))
        setupCard(binding.github, resources.getString(R.string.developer_github_url))
        setupCard(binding.instagram, resources.getString(R.string.developer_instagram_url))

        // EPubs Product Announcements
        setupCard(binding.epubsChanges, resources.getString(R.string.epubs_notice_url))

        // App Feedback
        binding.feedback.setOnClickListener {
            val send = Intent(Intent.ACTION_SENDTO)

            // Email subject (App Name and Version Code for troubleshooting)
            val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                    "?subject=" + Uri.encode("App Feedback: ") + resources.getString(R.string.app_name) + " " + versionName
            val uri = Uri.parse(uriText)
            send.data = uri

            // Displays apps that are able to handle email
            startActivity(Intent.createChooser(send, "Send feedback..."))
        }

        // Displays Google Play license builder
        binding.license.setOnClickListener {
            startActivity(
                Intent(
                    this@AboutActivity,
                    OssLicensesMenuActivity::class.java
                )
            )
            OssLicensesMenuActivity.setActivityTitle(getString(R.string.licenses))
        }

        // Gets user device info
        binding.device.setOnClickListener {
            showDeviceInfo()
        }

        // Long click to save device info to clipboard
        binding.device.setOnLongClickListener {
            Config.save(applicationContext, deviceInfoText)
            true
        }

        // Thank you acknowledgement
        binding.acknowledgement.setOnClickListener {
            thankYouEasterEgg()
        }
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
        Config.save(this, href)
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

    // Displays users device info
    private fun showDeviceInfo(){
        InfoSheet().show(this@AboutActivity){
            style(SheetStyle.DIALOG)
            title("Device Info")
            content(deviceInfoText)
            onPositive("Ok")
        }
    }

    // Thank you Easter Egg for the support from r/AirForce
    private fun thankYouEasterEgg(){
        InfoSheet().show(this@AboutActivity) {
            style(SheetStyle.DIALOG)
            title("Special Thanks and Consideration")
            content(R.string.thank_you_notice)
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
}
