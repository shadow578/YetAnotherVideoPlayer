# Yet Another Video Player
Yet Another Video Player (or YAVP) is a Video Player for Android that is based on Googles [ExoPlayer](https://github.com/google/ExoPlayer). <br/>
<img src="/ASSETS/screenshot_buckbunny_playui.png?raw=true" width="750">

## Who Is YAVP For?
First off, YAVP is not meant to compete with any of the big players like __VLC__ or __MX Player__. <br/>
<br/>
With that out of the way, here are some reasons you might want to consider using YAVP: 

* YAVP is fully __Open Source__
	* The sourcecode of YAVP and all libraries is open source and can be found [here](#used-librarys)
* YAVP __will (and cannot)__ spy on your data
	* See [here](#what-permissions-does-yavp-use) to see what permissions YAVP uses
* YAVP is __completely free and without ads__
* YAVP will play most __Video and Audio Files and Streams__
	* For a full list of supported formats, see [here](https://exoplayer.dev/supported-formats.html)
* __Simple and modern__ User Interface
	* No flashy lights and thousands of buttons, just a simple and tidy interface
* __Resume where I left__ Features
	* You can just close the app at any point - it will handle resuming for you later
* Build and Optimized for watchin __Anime__
	* For Anime- Specific features, see [here](#anime-specific-features)
	
## Things you should Know

* YAVP requires at least __Android 7.0 (Nougat)__
	* This may change in the future
* YAVP cannot be used standalone.
	* You will need to use another app (like the Gallery app) to open the videos you want to play
* While Anime4K really is great at making Anime look great, it is also outstandingly eager to drain your battery
	* When Anime4K is enabled, you will experience a __much__ higher battery drain
	* To slow down battery drain, you can enable FPS limiting in the app Settings
	* But don't worry, __YAVP__ will warn you when your battery is running out of charge (if enabled)
* YAVP is my very first Android App and my first (public) project on Github
	* If I'm doing something wrong, please feel free to let me know :)
	
## Anime Specific Features
I personally use YAVP to watch Anime. As a result of this, it has some features specifically built with Anime in mind.

* __Anime- Upscaling__ with [__Anime4K__](https://github.com/bloc97/Anime4K/)
	* Watch Anime like it was meant to be watched: in the highest possible quality
* __Skip Anime Openings__ with the click of a button
	* Skip that super annoying opening with YAVP's advanced seeking technology
* __More to come__

## FAQ

#### How Do I Play Videos using YAVP?
Simply open any Video file (for example in the Gallery app). Android will ask you what Video Player you want to use. Simply select YAVP and the video will start playing.
Additionally, you can share the video. In the Share menu, there will be a entry called __Play with YAVP.__

#### A App Forces Me To Use VLC For Videos
Take a look at [NotVLC](https://github.com/shadow578/NotVLC).

#### How Do I Use Swipe Gestures?
For Volume Control, swipe the right side of your screen Vertically. <br/>
For Brightness Control, swipe the left side of your screen Vertically.

#### How Do I Choose The Quality / Jump To A Position / Access The Settings?
All these functions can be accessed in the quick settings drawer.<br/>
To open the quick settings drawer, swipe from the right edge of the screen to the left. Make sure the play/pause button is visible when you do this.

#### How Do I Enable Anime4K?
Anime4K can be enabled in the effects drawer.<br/>
To open the effects drawer, swipe from the left edge of the screen to the right. Make sure the play/pause button is visible when you do this.

#### Will YAVP Stay Ad- Free?
99% Yes. Although Exoplayer supports adding Ads into Videos, I don't feel like this is something I'd like to do.

#### Does YAVP Use Trackers?
Nope.

#### How does YAVP Scale up Anime?
When enabled in the Quick Settings menu, YAVP will use [__bloc97's Anime4K__](https://github.com/bloc97/Anime4K/) for upscaling and enhancing Anime. 
This algorithm does, in a nutshell, look where there are Lines in the Video and makes the Lines appear sharper.<br/>
If you want to learn more on how Anime4K works, I suggest looking at [bloc97's Preprint](https://github.com/bloc97/Anime4K/blob/master/Preprint.md).

#### Why Only Anime4K 0.9?
Because this version of Anime4K is still _quite_ simple to understand and implement. <br/>
Additionally, I'm not sure if a mobile GPU could handle any of the newer versions.

## What Permissions Does YAVP Use?
Obviously, YAVP cannot function without having __any__ permissions.<br/>
These are all permissions that YAVP uses:

* [__INTERNET__](https://developer.android.com/reference/android/Manifest.permission#INTERNET)
	* To access the Internet and stream videos
* [__MODIFY_AUDIO_SETTINGS__](https://developer.android.com/reference/android/Manifest.permission#MODIFY_AUDIO_SETTINGS)
	* To adjust the Volume with gestures
* [__READ_EXTERNAL_STORAGE__](https://developer.android.com/reference/android/Manifest.permission#READ_EXTERNAL_STORAGE)
	* To play local videos
	* This permission is actually disabled by default and is only requested when needed.
	
There are also some permissions only used by features still in developement.<br/>
They are disabled by default and are __not__ requested.
* [__CAMERA__](https://developer.android.com/reference/android/Manifest.permission#CAMERA)
* [__Proximity Sensor__](https://developer.android.com/reference/android/hardware/Sensor#TYPE_PROXIMITY)
* [__Light Sensor__](https://developer.android.com/reference/android/hardware/Sensor#TYPE_LIGHT)

As you can see, YAVP cannot access any of your private information (for example [__READ_CONTACTS__](https://developer.android.com/reference/android/Manifest.permission#READ_CONTACTS).

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

* Write a detailed request, explaining what the feature should do and how it should do it
* Include Screenshots to illustrate your request (if needed)

## Used Librarys

* [google/ExoPlayer](https://github.com/google/ExoPlayer)
* [google/Material-Design-Icons](https://github.com/google/material-design-icons)
* [pnikosis/materialish-progress](https://github.com/pnikosis/materialish-progress)
* [svenwiegand/time-duration-picker](https://github.com/svenwiegand/time-duration-picker)
* [MasayukiSuda/ExoPlayerFilter](https://github.com/MasayukiSuda/ExoPlayerFilter)
* [bloc97/Anime4k](https://github.com/bloc97/Anime4K/)
	* I'm using a custom implementation based on Anime4K v0.9. For my implementation, see [here](https://github.com/shadow578/Anime4kSharp)
