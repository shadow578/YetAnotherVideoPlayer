package de.shadow578.yetanothervideoplayer.feature.update;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.shadow578.yetanothervideoplayer.BuildConfig;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * A default implementation of the {@link AppUpdateManager.UpdateCallback}
 */
public abstract class DefaultUpdateCallback implements AppUpdateManager.UpdateCallback
{
    @Override
    public AppUpdateManager.VersionComparison compareVersion(String version)
    {
        //version is in format "v###", so we need to parse it to a version int first
        //this regex does that, and more. It ensures that the string is in format "v####", and capture group 1 contains the version int.
        //would be easier to just not put a v before the version, but that's boring ;)
        Pattern versionIntRegex = Pattern.compile("^v(\\d{3,})$");
        Matcher match = versionIntRegex.matcher(version);

        //check format first
        if (!match.find())
        {
            Logging.logW("Could not match version string %s against pattern v####", version);
            return AppUpdateManager.VersionComparison.INVALID;
        }

        //get result of the match to get cg1
        MatchResult result = match.toMatchResult();
        if (result.groupCount() < 1)
        {
            //sanity check: this should never really happen
            Logging.logW("Match Result for version string %s does not have at least one match group!", version);
            return AppUpdateManager.VersionComparison.INVALID;
        }

        //get version int from cg1 (as a string)
        String versionIntStr = result.group(1);
        if (versionIntStr == null || versionIntStr.isEmpty())
        {
            //cg1 was empty! cannot parse nothing ;)
            Logging.logW("Capture Group 1 for version string %s was empty!", version);
            return AppUpdateManager.VersionComparison.INVALID;
        }

        //parse version as int
        int versionInt;
        try
        {
            versionInt = Integer.parseInt(versionIntStr);
        }
        catch (NumberFormatException e)
        {
            Logging.logW("Capture Group 1 string %s of version string %s failed to parse: ", versionIntStr, version);
            e.printStackTrace();
            return AppUpdateManager.VersionComparison.INVALID;
        }

        //compare version int to current version
        int appVersion = BuildConfig.VERSION_CODE;
        if (versionInt == appVersion)
        {
            //app is same version
            Logging.logE("same version");
            return AppUpdateManager.VersionComparison.SAME_VERSION;
        }
        else if (versionInt > appVersion)
        {
            //app is older version
            Logging.logE("newer version");
            return AppUpdateManager.VersionComparison.NEWER_VERSION;
        }
        else
        {
            //app is newer version (eg. debug / dev build)
            Logging.logE("older version");
            return AppUpdateManager.VersionComparison.OLDER_VERSION;
        }

        //return AppUpdateManager.VersionComparison.NEWER_VERSION;
        //return version.equals("v881") ? AppUpdateManager.VersionComparison.SAME_VERSION : AppUpdateManager.VersionComparison.NEWER_VERSION;
    }
}
