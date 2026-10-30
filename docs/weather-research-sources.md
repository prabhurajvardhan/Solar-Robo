# Weather Intelligence Sources and Evidence

This note records external source facts used in the ASTRA/Solar Robo weather work.

## WEBCAM training dataset

The official [WEBCAM dataset repository](https://github.com/MarcusCoteFIT/webcam-ground-based-cloud-image-dataset) describes more than 15,500 labeled ground-based images in nine folders/classes: altocumulus (`Ac`), cumulonimbus (`Cb`), cirrus (`Ci`), clear (`Clear`), fair-weather cumulus (`Cu`), fog/obscuration (`Obsc`), rain/precipitation (`Precip`), stratus (`St`), and towering cumulus (`TCu`). Its README states the dataset is publicly available for research purposes but does not specify a dataset license or detailed reuse/redistribution terms.

The [official v1.0 GitHub release](https://github.com/MarcusCoteFIT/webcam-ground-based-cloud-image-dataset/releases/tag/v1.0) links `WEBCAM_v1.zip`, listed as 1.82 GB, with published SHA-256 `44aadeb0b8b68cdbb8a222f561d69e398a03e96b0e37465559a98ab3eccd3201`. The local archive checksum matched that release digest. The release is not redistributed in this project.

The associated IEEE Access [paper](https://doi.org/10.1109/ACCESS.2025.3634057) says the dataset and training code are public for research purposes. The article itself is CC BY-NC-ND, but that article license is not evidence that the image archive has the same license. The paper identifies five hazardous-related classes (`Cb`, `Obsc`, `Precip`, `St`, `TCu`) and four generally nonhazardous classes (`Ac`, `Ci`, `Clear`, `Cu`), and reports a 97.1% validation accuracy for its separate ensemble. Those reported results are not results of this project.

The project task's seven operational classes map directly to seven WEBCAM folders: Clear→clear, Cu→fair_weather_cumulus, St→overcast_or_stratus, TCu→towering_cumulus, Cb→cumulonimbus_or_storm_like, Obsc→fog_or_obscured_sky, Precip→rain_or_precipitation. `Ac` and `Ci` have no direct target class and are excluded rather than forced into an unsupported category. These are visual classes, not calibrated storm probabilities or measured surface weather.

## Alternate CC-BY dataset reviewed

The [Zenodo Montenegro all-sky dataset](https://zenodo.org/records/21787669) describes 2,522 images from one camera and 11,191 meteorological observer annotations and declares CC BY 4.0. Its annotations are WMO sky-observation variables, not direct labels for rain/precipitation or fog cause; its README also contains unfilled template fields. It was not selected for the seven-label classifier. The [TJNU repository](https://github.com/shuangliutjnu/TJNU-Ground-based-Cloud-Dataset) requires acceptance of a separate agreement with restrictions on redistribution and modification; it was not used.

## Forecast source

The [Open-Meteo forecast documentation](https://open-meteo.com/en/docs) documents hourly cloud cover, precipitation, and 10 m wind forecast fields. Its [free service terms](https://open-meteo.com/en/terms) limit the no-cost API to non-commercial use, specify request-rate limits, and require CC BY 4.0 attribution. Open-Meteo's response used here has no provider model issuance timestamp; the app must label retrieval time as a proxy, not claim true issue time.

## Climatology evidence

The [India Meteorological Department cyclone information page](https://mausam.imd.gov.in/responsive/cycloneinformation.php) was identified as the intended authoritative operational source but timed out during direct inspection, so no verified local table was copied into the app. The peer-reviewed [Bay of Bengal seasonal forecast study](https://rmets.onlinelibrary.wiley.com/doi/10.1002/joc.7651) identifies October–December as the post-monsoon cyclone season in the Bay of Bengal and discusses regional seasonal cyclone risk. It is not a site-specific wind/climatology table. The runtime therefore defaults to an explicitly provisional user-configurable profile until a verified location-specific source is supplied.
