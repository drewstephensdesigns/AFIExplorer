package com.drewcodesit.afiexplorer.models

data class FilterOption(
    val displayName: String, // what the user sees in the UI
    val filterValue: String   // what gets passed to the adapter for filtering
)
object Filters{
    // Department Level
    val externalOrg = listOf(
        FilterOption("Department of Defense", "DoD"),
        FilterOption("LeMay Center", "LeMay Center")
    )

    val organizations = listOf(
        FilterOption("Headquarters Air Force", "HAF"),
        FilterOption("Headquarters Space Force", "USSF")
    )

    // Major Commands
    val commands = listOf(
        FilterOption("ACC", "ACC"),
        FilterOption("AMC", "AMC"),
        FilterOption("AETC", "AETC"),
        FilterOption("PACAF", "PACAF"),
        FilterOption("USAFE-AFAFRICA", "USAFE-AFAFRICA"),
        FilterOption("AFGSC", "AFGSC"),
        FilterOption("AFMC", "AFMC"),
        FilterOption("AFRC", "AFRC"),
        FilterOption("AFSOC", "AFSOC"),
        FilterOption("ANG", "ANG"),
        FilterOption("Space Force/CFC", "CFC"),
        FilterOption("Space Force/SSC", "SSC"),
        FilterOption("Space Force/STARCOM", "STARCOM"),
        FilterOption("Space Force/COO", "COO"),
        FilterOption("Space Force/CSRO", "CSRO")
    )

    // Bases
    val bases = listOf(
        FilterOption("Altus AFB", "AltusAFB"),
        FilterOption("Arnold AFB", "ArnoldAFB"),
        FilterOption("Aviano AB", "AvianoAB"),
        FilterOption("Barksdale AFB", "BarksdaleAFB"),
        FilterOption("Beale AFB", "BealeAFB"),
        FilterOption("Buckley SFB", "BuckleySFB"),
        FilterOption("Cannon AFB", "CannonAFB"),
        FilterOption("Cheyenne Mountain AS", "CheyenneMountainAS"),
        FilterOption("Columbus AFB", "ColumbusAFB"),
        FilterOption("Creech AFB", "CreechAFB"),
        FilterOption("Davis-Monthan AFB", "Davis-MonthanAFB"),
        FilterOption("Dobbins ARB", "DobbinsARB"),
        FilterOption("Dover AFB", "DoverAFB"),
        FilterOption("Dyess AFB", "DyessAFB"),
        FilterOption("Edwards AFB", "EdwardsAFB"),
        FilterOption("Eglin AFB", "EglinAFB"),
        FilterOption("Eielson AFB", "EielsonAFB"),
        FilterOption("Ellsworth AFB", "EllsworthAFB"),
        FilterOption("Fairchild AFB", "FairchildAFB"),
        FilterOption("Francis E. Warren AFB", "FrancisEWarrenAFB"),
        FilterOption("Goodfellow AFB", "GoodfellowAFB"),
        FilterOption("Grand Forks AFB", "GrandForksAFB"),
        FilterOption("Hill AFB", "HillAFB"),
        FilterOption("Holloman AFB", "HollomanAFB"),
        FilterOption("Homestead ARB", "HomesteadARB"),
        FilterOption("Hurlburt Field", "HurlburtField"),
        FilterOption("Incirlik AB", "IncirlikAb"),
        FilterOption("Joint Base Anacostia-Bolling", "JBAB"),
        FilterOption("JB Andrews – NAF Washington DC", "JBAndrews-NAF Washington DC"),
        FilterOption("Joint Base Charleston", "JBCharleston"),
        FilterOption("Joint Base Elmendorf-Richardson", "JBElmendorf-Richardson"),
        FilterOption("Joint Base Langley-Eustis", "JBLangley-Eustis"),
        FilterOption("Joint Base Lewis-McChord", "JBLewis-McChord"),
        FilterOption("JB McGuire-Dix-Lakehurst", "JBMcGuire-Dix-Lakehurst"),
        FilterOption("JB Pearl Harbor-Hickam", "JBPearl Harbor-Hickam"),
        FilterOption("Joint Base San Antonio", "JBSA"),
        FilterOption("Kadena AB", "KadenaAB"),
        FilterOption("Keesler AFB", "KeeslerAFB"),
        FilterOption("Kirtland AFB", "KirtlandAFB"),
        FilterOption("Lajes Field", "LajesField"),
        FilterOption("RAF Lakenheath", "Lakenheath"),
        FilterOption("Laughlin AFB", "LaughlinAFB"),
        FilterOption("Little Rock AFB", "LittlerockAFB"),
        FilterOption("Los Angeles AFB", "LosAngelesAFB"),
        FilterOption("Luke AFB", "LukeAFB"),
        FilterOption("MacDill AFB", "MacDillAFB"),
        FilterOption("Malmstrom AFB", "MalmstromAFB"),
        FilterOption("March ARB", "MarchARB"),
        FilterOption("Maxwell AFB", "MaxwellAFB"),
        FilterOption("McConnell AFB", "McConnellAFB"),
        FilterOption("McGuire AFB", "McGuireAFB"),
        FilterOption("Minot AFB", "MinotAFB"),
        FilterOption("Misawa AB", "MISAWAAB"),
        FilterOption("Moody AFB", "MoodyAFB"),
        FilterOption("Mountain Home AFB", "MountainHomeAFB"),
        FilterOption("Nellis AFB", "NellisAFB"),
        FilterOption("Offutt AFB", "OffuttAFB"),
        FilterOption("Osan AB", "Osan AB"),
        FilterOption("Patrick AFB", "Patrick AFB"),
        FilterOption("Peterson AFB", "PetersonAFB"),
        FilterOption("Pope AAF", "POPEAA"),
        FilterOption("RAF Mildenhall", "RAFMILDENHALL"),
        FilterOption("Ramstein AB", "RamsteinAB"),
        FilterOption("Robins AFB", "RobinsAFB"),
        FilterOption("Schriever AFB", "SchrieverAFB"),
        FilterOption("Scott AFB", "ScottAFB"),
        FilterOption("Seymour Johnson AFB", "SeymourJohnsonAFB"),
        FilterOption("Shaw AFB", "ShawAFB"),
        FilterOption("Sheppard AFB", "SheppardAFB"),
        FilterOption("SBD 1", "SBD 1"),
        FilterOption("Spangdahlem AB", "SpangdahlemAB"),
        FilterOption("Thule AB", "ThuleAB"),
        FilterOption("Tinker AFB", "TinkerAFB"),
        FilterOption("Travis AFB", "TravisAFB"),
        FilterOption("Tyndall AFB", "TyndallAFB"),
        FilterOption("Vance AFB", "VanceAFB"),
        FilterOption("Whiteman AFB", "WhitemanAFB"),
        FilterOption("Wright-Patterson AFB", "WrightPattersonAFB"),
        FilterOption("Yokota AB", "YokotaAB")
    )
}