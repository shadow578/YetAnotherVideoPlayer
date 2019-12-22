# Yet Another Video Player
Yet Another Video Player is a Video Player based on Googles Exoplayer. <br/>
<img src="/ASSETS/screenshot-framed.png?raw=true" width="750">


## Why YAVP?
YAVP is not meant to compete with any of the big players like __VLC__ or __MX Player__. <br/>
That being said, here are some reasons you might want ot use YAVP:

* YAVP is fully __Open Source__
* YAVP __will (and cannot)__ spy on you!
	* YAVP does not require any Permissions
* YAVP is __completely free without Ads__
* YAVP will play most __Video and Audio Files aswell as Online Streams__
	* YAVP supports most mayor file formats and Streaming Technologies
* __Modern and Intuitive__ User Interface with __Swipe Gestures__ for Volume & Brightness
* __Resume where I left__- Feature
	* If the App should crash, you can just resume watching with the press of a button
* __Anime__ upscaling and enhancing using [Anime4K](https://github.com/bloc97/Anime4K/)
	* YAVP is currently the __only__ Video Player for Android that supports this

## Things you should Know
* YAVP requires at least __Android 7.0 (Nougat)__ (this may change in the future)
* YAVP currently cannot be used stand- alone. You will need another app (like Gallery) that lets you select the videos you want to play
* While Anime4K is great at making Anime look great, it is also outstandingly eager to eat your battery
	* When Anime4K is enabled, you can expect a __much__ higher battery usage
	* To prolong your battery life, you can enable FPS limiting in the Settings
	* _YAVP is friendly enough to warn you when your battery is getting low_ (if enabled)
* YAVP is my very first Android App and my first public project on Github
	* If I'm doing something wrong, please feel free to let me know :)


## FAQ
#### How Do I Play Videos using YAVP?
Simply open any Video file (for example in the Gallery app). Android will ask you what Video Player you want to use. Simply select YAVP here and the video will start playing.
Additionally, you can share the video. In the Share menu, there will be a entry called __Play with YAVP.__

#### How Do I Use Swipe Gestures?
For Volume Control, swipe the right side of your screen Vertically. <br/>
For Brightness Control, swipe the left side of your screen Vertically.

#### How Do I Access The Quick Settings Menu?
To access the quick settings menu, swipe to the left from the right edge of your screen. This will reveal the Quick Settings Menu.

#### What Permissions Does YAVP Actually Need?
Obviously, YAVP cannot function without having any permissions. This are all permissions YAVP requires:
* __INTERNET__ - To connect to the Internet and stream videos
* __READ_EXTERNAL_STORAGE__ - To play local files
	* This permission is actually disabled by default and is only requested when needed.
* __MODIFY_AUDIO_SETTINGS__ - To adjust the volume with swipe gestures

As you can see, YAVP cannot access any of your private information (for example __read contacts__).

#### Will YAVP Stay Ad- Free?
Probably. Although the Exoplayer library supports inserting Ads into Videos, I don't feel like this is something I'm able to do.

#### How does YAVP Scale up Anime?
When enabled in the Quick Settings menu, YAVP will use [__bloc97's Anime4K__](https://github.com/bloc97/Anime4K/) for upscaling and enhancing Anime. 
This algorithm does, in a nutshell, look where there are Lines in the Video and makes the Lines appear sharper.

If you want to learn more on how Anime4K works, I suggest looking at [bloc97's Preprint](https://github.com/bloc97/Anime4K/blob/master/Preprint.md).

#### Why Only Anime4K 0.9?
Because this version of Anime4K is still _quite_ simple to understand and implement. <br/>
Additionally, I'm not sure if a mobile GPU could handle any of the newer versions.



## Issues, Feature Requests and Contributing
Please make sure to read these guidelines. Your issue may be closed without a warning if you don't.

#### Bugs

* **Include the App Version**
	* If not on the latest App Version, try updating the app. The Issue may have already been solved
* Include Steps to reproduce the Bug** (if not obvious from the Description)
* Include Screenshots (if needed)
* If you suspect the Bug may be device specific, try reproducing on another device (if possible)
* For large logs use [pastebin.com](https://pastebin.com/) or similar
* Don't group unrelated requests into one issue

#### Feature Requests
* Write a detailed issue, explaining what (or how) the Feature should
	* Avoid writing "...like (this) app does"
* Include Screenshots (if needed)


## Used Librarys
* [google/ExoPlayer](https://github.com/google/ExoPlayer)
* [google/Material-Design-Icons](https://github.com/google/material-design-icons)
* [pnikosis/materialish-progress](https://github.com/pnikosis/materialish-progress)
* [svenwiegand/time-duration-picker](https://github.com/svenwiegand/time-duration-picker)
* [MasayukiSuda/ExoPlayerFilter](https://github.com/MasayukiSuda/ExoPlayerFilter)
* [bloc97/Anime4k](https://github.com/bloc97/Anime4K/)
	* I'm using a custom implementation based on Anime4K v0.9
