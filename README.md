# 📄 Re:T-UI-PDF

A terminal-inspired, zero-bloat, cyberdeck PDF reader built specifically for the **Re:T-UI Launcher ecosystem** by DvilSpawn.

---

## ⚡ Features (Zero AI Slop)

* **🎨 100% Re:T-UI Theme Auto-Inherit:** Automatically reads the active theme from `/sdcard/Android/data/com.dvil.tui_renewed/files/Re-T-UI/theme.xml` (or legacy `/sdcard/t-ui/theme.xml`). Instant sync with **Cyberpunk**, **Dracula**, **Synthwave**, **OLED**, **Pip-Boy Amber**, **Matrix**, and **Catppuccin**.
* **📟 Phosphor Color Inversion Shader:** Inverts standard blinding white PDF pages into pure black canvas with active theme phosphor text (Matrix Green, Pip-Boy Amber, Laser Cyan, or OLED White).
* **🖥️ Terminal Window & Function Key Bar:**
  * Top Header: `[READY · PDF: lecture-03.pdf] [Pg: 14/82] [PHOSPHOR]` with 1px cyber border.
  * Bottom F-Keys: `[F1 Help] [F4 Invert] [F5 Prev] [F6 Next] [F10 Quit]`.
  * Keyboard navigation: Vim keys (`h`/`l`, `j`/`k`, `q` to quit, `i` to invert).
* **🚀 Instant Zero-Delay Startup:** Transparent 1px splash screen for immediate terminal response.
* **🔗 Deep Linking Intent Contract:**
  * Action: `com.dvil.retui.pdf.OPEN_CONSOLE`
  * Standard View: `android.intent.action.VIEW` for `application/pdf`.

---

## 🛠️ Automated Cloud Build (GitHub Actions)

You don't need Android Studio or a heavy PC. Just push this repository to GitHub:

```bash
git init
git add .
git commit -m "feat: initial Re:T-UI PDF cyberdeck viewer"
git remote add origin https://github.com/<your-username>/Re-T-UI-PDF.git
git branch -M main
git push -u origin main
```

### Download the APK:
1. Go to your repository's **Actions** tab on GitHub.
2. Click on the latest workflow run: **Build Re:T-UI PDF Release APK**.
3. Under **Artifacts**, download `Re-T-UI-PDF-release.zip` which contains your installable `app-release.apk`!
4. Or push a tag (e.g. `git tag v1.0.0 && git push --tags`) to automatically generate a formal GitHub Release.
