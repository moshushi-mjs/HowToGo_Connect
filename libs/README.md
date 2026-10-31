# `libs/`

放问道（HowToGo）的 jar，编译时用。

```
libs/howtogo-<版本>.jar
```

文件名以 `howtogo-` 开头、以 `.jar` 结尾就会被 `build.gradle` 找到，不用改构建脚本。

`libs/*.jar` 在 `.gitignore` 里，不会进仓库。
