package android.taobao.windvane.config;

import com.huawei.hms.support.api.entity.core.JosStatusCodes;

public class WVCommonConfigData {
    public String[] aliNetworkDegradeDomains = new String[0];
    public String cookieUrlRule = "";
    public int customsComboLimit = 1;
    public int customsDirectQueryLimit = 10;
    public long disableInstallPeriod_end = 0;
    public long disableInstallPeriod_start = 0;
    public int downloadCoreType = 3;
    public boolean enableUCPrecache = false;
    public boolean enableUCPrecacheDoc = false;
    public boolean enableUcShareCore = true;
    public String excludeUCVersions = "1.12.11.0, 1.15.15.0, 1.14.13.0, 1.13.12.0";
    public boolean firstUseSystemWebViewOn7zInit = false;
    public int gpuMultiPolicy = 0;
    public String initOldCoreVersions = "3.*";
    public int initUCCorePolicy = 0;
    public int initWebPolicy = 19;
    public boolean isAutoRegisterApp = false;
    public boolean isCheckCleanup = true;
    public boolean isOpenCombo = true;
    public boolean isUseAliNetworkDelegate = true;
    public boolean isUseTBDownloader = true;
    public int monitorStatus = 2;
    public String[] monitoredApps = new String[0];
    public boolean needZipDegrade = false;
    public boolean openLog = false;
    public int packageAccessInterval = 3000;
    public int packageAppStatus = 2;
    public int packageDownloadLimit = 30;
    public int packageMaxAppCount = 100;
    public double packagePriorityWeight = 0.1d;
    public int packageRemoveInterval = 432000000;
    public String packageZipPrefix = "";
    public String packageZipPreviewPrefix = "";
    public String precachePackageName = "";
    public int recoveryInterval = 432000000;
    public String shareBlankList = "";
    public String ucCoreUrl = "";
    public boolean ucMultiServiceSpeedUp = false;
    public int ucMultiTimeOut = JosStatusCodes.RTN_CODE_COMMON_ERROR;
    public UCParamData ucParam = new UCParamData("{}");
    public boolean ucSkipOldKernel = true;
    public double ucsdk_alinetwork_rate = 1.0d;
    public double ucsdk_image_strategy_rate = 1.0d;
    public long updateInterval = 300000;
    public int urlRuleStatus = 2;
    public String urlScheme = "http";
    public boolean useSystemWebView = false;
    public boolean useUCPlayer = false;
    public String v = "0";
    public String verifySampleRate = null;
    public int webMultiPolicy = 0;
    public String zipDegradeList = "";
    public int zipDegradeMode = 0;
}
