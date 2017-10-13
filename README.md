# DownloadUtil
Android平台多线程下载工具，支持断点续传！

Android platform multi-threaded download tool to support breakpoint continuo!

## Gradle

```
1.Add it in your root build.gradle at the end of repositories （添加maven仓库到根目录下的build.gradle）:

allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}

2.Add the dependency（添加依赖到app下的build.gradle）

dependencies {
	...
	compile 'com.github.xuehuiniaoyu:DownloadUtil:[Latest release]'
}

```

## Maven

```
1.
<repositories>
	<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
	</repository>
</repositories>

2.
<dependency>
    <groupId>com.github.xuehuiniaoyu</groupId>
    <artifactId>DownloadUtil</artifactId>
    <version>[Latest release]</version>
</dependency>

