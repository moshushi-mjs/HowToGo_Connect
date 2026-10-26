#  问道互联（htg_connect）—— howtogo 接口兼容性检查
#
#  作用
#    本模组深度依赖 howtogo 的内部类。howtogo 一升级，这些类的签名可能变，
#    而现在只能等游戏启动崩了才发现。这个脚本把「等崩」变成「跑一下就知道」。
#
#  用法
#    powershell -ExecutionPolicy Bypass -File tools\check-api.ps1
#    powershell -ExecutionPolicy Bypass -File tools\check-api.ps1 路径\howtogo-0.2.3.jar
#
#    不给参数时，自动用 libs\howtogo-*.jar 里最新的那个。
#
#  退出码
#    0 = 全部通过
#    1 = 有缺失（会列出缺的是哪个类、哪个方法）

param(
    [string]$JarPath = ""
)

$ErrorActionPreference = "Continue"

#  1. 找要检查的 jar
if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $libs = Join-Path (Split-Path -Parent $PSScriptRoot) "libs"
    $candidates = @()
    if (Test-Path $libs) {
        $candidates = Get-ChildItem $libs -Filter "howtogo-*.jar" -File |
            Where-Object { $_.Name -notmatch "\.bak$" } |
            Sort-Object Name -Descending
    }
    if ($candidates.Count -eq 0) {
        Write-Host "  找不到 libs\howtogo-*.jar" -ForegroundColor Red
        Write-Host "  用法: check-api.ps1 <howtogo 的 jar 路径>" -ForegroundColor DarkGray
        exit 1
    }
    $JarPath = $candidates[0].FullName
}

if (-not (Test-Path $JarPath)) {
    Write-Host "  文件不存在: $JarPath" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "  检查目标: $(Split-Path -Leaf $JarPath)" -ForegroundColor Cyan
Write-Host "            $JarPath" -ForegroundColor DarkGray

#  2. 找 javap
$javap = $null
$javaHomes = @(
    $env:JAVA_HOME,
    "C:\Program Files\Java\jdk-21.0.10",
    "C:\Program Files\Java\jdk-21"
)
foreach ($jdkHome in $javaHomes) {
    if ([string]::IsNullOrWhiteSpace($jdkHome)) { continue }
    $probe = Join-Path $jdkHome "bin\javap.exe"
    if (Test-Path $probe) { $javap = $probe; break }
}
if (-not $javap) {
    $cmd = Get-Command javap -ErrorAction SilentlyContinue
    if ($cmd) { $javap = $cmd.Source }
}
if (-not $javap) {
    Write-Host "  找不到 javap.exe —— 请设置 JAVA_HOME 或把 JDK 的 bin 加进 PATH" -ForegroundColor Red
    exit 1
}

#  3. 要核对的清单
#    每一项: 类名 @ 需要在 javap 输出里出现的片段 @ 用途说明
$checks = @(
    @{ Class = "bili.dongsz.howtogo.client.RoadStore"; Need = "RoadNetwork get()";        Use = "读当前路网（核心）" }
    @{ Class = "bili.dongsz.howtogo.client.RoadStore"; Need = "void markDirty()";         Use = "标记存盘" }
    @{ Class = "bili.dongsz.howtogo.road.RoadStorage"; Need = "RoadNetwork load(";        Use = "解析路网文件" }
    @{ Class = "bili.dongsz.howtogo.road.RoadStorage"; Need = "boolean save(";            Use = "序列化路网（房主开房用）" }
    @{ Class = "bili.dongsz.howtogo.road.RoadNetwork"; Need = "int revision()";           Use = "版本号（判断有没有被改过）" }
    @{ Class = "bili.dongsz.howtogo.road.RoadNetwork"; Need = "void replaceWith(";        Use = "整个替换路网（核心）" }
    @{ Class = "bili.dongsz.howtogo.road.RoadNetwork"; Need = "int nodeCount()";          Use = "统计点数" }
    @{ Class = "bili.dongsz.howtogo.road.RoadNetwork"; Need = "int segmentCount()";       Use = "统计线段数" }
    @{ Class = "bili.dongsz.howtogo.api.WorldDataApi"; Need = "int roadRevision()";       Use = "路网版本号（api 包）" }
    @{ Class = "bili.dongsz.howtogo.client.WorldFiles"; Need = "Path of(";                Use = "当前维度的路网文件路径（局域网用）" }
)

#  4. 逐个核对
$cache = @{}
$failed = @()
$passed = 0

foreach ($check in $checks) {
    $class = $check.Class

    if (-not $cache.ContainsKey($class)) {
        $output = & $javap -cp $JarPath $class 2>$null | Out-String
        $exists = ($LASTEXITCODE -eq 0) -and ($output -notmatch "^\s*$")
        $cache[$class] = @{ Exists = $exists; Text = $output }
    }

    $entry = $cache[$class]
    $shortClass = $class -replace "^.*\.", ""

    if (-not $entry.Exists) {
        Write-Host ("  [X] {0,-14} {1}" -f $shortClass, "整个类都没了！") -ForegroundColor Red
        $failed += "$shortClass 类不存在"
        continue
    }

    if ($entry.Text -match [regex]::Escape($check.Need)) {
        Write-Host ("  [OK] {0,-14} {1,-28} {2}" -f $shortClass, $check.Need, $check.Use) -ForegroundColor Green
        $passed++
    } else {
        Write-Host ("  [X]  {0,-14} {1,-28} {2}" -f $shortClass, $check.Need, $check.Use) -ForegroundColor Red
        $failed += "$shortClass.$($check.Need)"
    }
}

#  5. 顺手报一下格式版本（不参与判定，只是给人看的）
$fmt = & $javap -cp $JarPath -constants bili.dongsz.howtogo.road.RoadStorage 2>&1 | Out-String
if ($fmt -match "FORMAT_VERSION\s*=\s*(\d+)") {
    Write-Host ""
    Write-Host "  路网文件格式版本 FORMAT_VERSION = $($Matches[1])" -ForegroundColor DarkGray
    Write-Host "  （我们生成/读取的路网文件用这个版本号，变了就要检查兼容性）" -ForegroundColor DarkGray
}

# 总结
Write-Host ""
if ($failed.Count -eq 0) {
    Write-Host "  ===== 全部通过（$passed / $($checks.Count)） =====" -ForegroundColor Green
    Write-Host "  这个版本的 howtogo 可以直接用，重新编译即可。" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  ===== 有 $($failed.Count) 项不对 =====" -ForegroundColor Red
    foreach ($item in $failed) {
        Write-Host "    - $item" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "  改法：在 src\main\java 里搜索对应的类名/方法名，逐个改。" -ForegroundColor Yellow
    Write-Host "  清单见 Plan.md 的「用到的问道内部类」一节。" -ForegroundColor Yellow
    exit 1
}
