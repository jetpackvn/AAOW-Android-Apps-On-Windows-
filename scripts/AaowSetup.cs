using System;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using Microsoft.Win32;

namespace AaowSetup
{
    internal static class Program
    {
        private const string AppName = "AAOW";
        private const string AppDisplayName = "AAOW — Android Apps On Windows";
        private const string AppVersion = "1.0.0";
        private const string ProgId = "AAOW.ApkPackage";
        private const string UninstallSubKey = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\AAOW";

        [DllImport("shell32.dll")]
        private static extern void SHChangeNotify(uint wEventId, uint uFlags, IntPtr dwItem1, IntPtr dwItem2);

        [STAThread]
        private static int Main(string[] args)
        {
            bool silent = false;
            bool uninstall = false;
            bool createShortcuts = true;
            bool associateApk = true;
            string targetDir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "AAOW");

            foreach (string raw in args)
            {
                string a = raw != null ? raw.Trim().Trim('"') : "";
                string lower = a.ToLowerInvariant();
                if (lower == "/s" || lower == "--silent")
                {
                    silent = true;
                }
                else if (lower == "/u" || lower == "/uninstall" || lower == "--uninstall")
                {
                    uninstall = true;
                }
                else if (lower == "/noassoc")
                {
                    associateApk = false;
                }
                else if (lower == "/noshortcut")
                {
                    createShortcuts = false;
                }
                else if (lower.StartsWith("/d="))
                {
                    targetDir = a.Substring(3).Trim().Trim('"');
                }
                else if (lower.StartsWith("--dir="))
                {
                    targetDir = a.Substring(6).Trim().Trim('"');
                }
            }

            if (uninstall)
            {
                return RunUninstall(targetDir, silent);
            }

            if (silent)
            {
                try
                {
                    PerformInstall(targetDir, createShortcuts, associateApk, null);
                    return 0;
                }
                catch (Exception ex)
                {
                    try
                    {
                        File.WriteAllText(Path.Combine(Path.GetTempPath(), "aaow-setup-error.log"), ex.ToString(), Encoding.UTF8);
                    }
                    catch { }
                    Console.Error.WriteLine("Install error: " + ex);
                    return 1;
                }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new SetupForm(targetDir));
            return 0;
        }

        internal static void PerformInstall(string installDir, bool createShortcuts, bool associateApk, Action<int, string> progress)
        {
            if (string.IsNullOrWhiteSpace(installDir))
            {
                throw new ArgumentException("Thư mục cài đặt không hợp lệ.");
            }
            installDir = Path.GetFullPath(installDir);
            Directory.CreateDirectory(installDir);

            Report(progress, 10, "Đang giải nén AAOW Runtime & Ứng dụng...");
            string tempZip = Path.Combine(Path.GetTempPath(), "aaow-payload-" + Guid.NewGuid().ToString("N") + ".zip");
            try
            {
                ExtractResourceToFile("AAOW-payload.zip", tempZip);
                Report(progress, 35, "Đang cài đặt các tệp vào " + installDir + "...");
                using (ZipArchive archive = ZipFile.OpenRead(tempZip))
                {
                    int total = Math.Max(1, archive.Entries.Count);
                    int done = 0;
                    foreach (ZipArchiveEntry entry in archive.Entries)
                    {
                        string rel = entry.FullName.Replace('/', Path.DirectorySeparatorChar);
                        if (string.IsNullOrEmpty(rel)) continue;
                        string destPath = Path.GetFullPath(Path.Combine(installDir, rel));
                        if (!destPath.StartsWith(installDir, StringComparison.OrdinalIgnoreCase)) continue;
                        if (entry.FullName.EndsWith("/") || entry.FullName.EndsWith("\\") || string.IsNullOrEmpty(entry.Name))
                        {
                            Directory.CreateDirectory(destPath);
                        }
                        else
                        {
                            string parent = Path.GetDirectoryName(destPath);
                            if (!string.IsNullOrEmpty(parent)) Directory.CreateDirectory(parent);
                            entry.ExtractToFile(destPath, true);
                        }
                        done++;
                        if (done % 15 == 0)
                        {
                            int pct = 35 + (done * 45 / total);
                            Report(progress, Math.Min(80, pct), "Đang giải nén (" + done + "/" + total + ")...");
                        }
                    }
                }
            }
            finally
            {
                try { if (File.Exists(tempZip)) File.Delete(tempZip); } catch { }
            }

            Report(progress, 82, "Đang trích xuất biểu tượng & trình gỡ cài đặt...");
            string apkIconPath = Path.Combine(installDir, "apk-file.ico");
            if (!File.Exists(apkIconPath))
            {
                TryExtractResourceToFile("apk-file.ico", apkIconPath);
            }
            string aaowIconPath = Path.Combine(installDir, "aaow.ico");
            if (!File.Exists(aaowIconPath))
            {
                TryExtractResourceToFile("aaow.ico", aaowIconPath);
            }

            string selfExe = Assembly.GetExecutingAssembly().Location;
            string uninstallerPath = Path.Combine(installDir, "Uninstall-AAOW.exe");
            if (!string.Equals(Path.GetFullPath(selfExe), Path.GetFullPath(uninstallerPath), StringComparison.OrdinalIgnoreCase))
            {
                File.Copy(selfExe, uninstallerPath, true);
            }

            string exePath = Path.Combine(installDir, "AAOW.exe");
            if (associateApk)
            {
                Report(progress, 90, "Đang đăng ký liên kết đuôi file .apk trên Windows...");
                RegisterApkAssociation(exePath, File.Exists(apkIconPath) ? apkIconPath : exePath + ",0");
            }

            if (createShortcuts)
            {
                Report(progress, 95, "Đang tạo lối tắt Start Menu & Desktop...");
                CreateShortcut(
                    Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "AAOW App Hub.lnk"),
                    exePath, installDir, aaowIconPath, "Chạy ứng dụng Android trực tiếp trên Windows");
                CreateShortcut(
                    Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "AAOW App Hub.lnk"),
                    exePath, installDir, aaowIconPath, "Chạy ứng dụng Android trực tiếp trên Windows");
            }

            RegisterUninstallEntry(installDir, exePath, uninstallerPath, aaowIconPath);
            Report(progress, 100, "Cài đặt AAOW hoàn tất!");
        }

        private static int RunUninstall(string fallbackDir, bool silent)
        {
            string installDir = fallbackDir;
            try
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey(UninstallSubKey))
                {
                    if (key != null)
                    {
                        object loc = key.GetValue("InstallLocation");
                        if (loc != null && !string.IsNullOrWhiteSpace(loc.ToString()))
                        {
                            installDir = loc.ToString();
                        }
                    }
                }
            }
            catch { }

            if (!silent)
            {
                DialogResult res = MessageBox.Show(
                    "Bạn có muốn gỡ cài đặt " + AppDisplayName + " khỏi:\n" + installDir + "?",
                    "Gỡ cài đặt AAOW",
                    MessageBoxButtons.YesNo,
                    MessageBoxIcon.Question);
                if (res != DialogResult.Yes) return 0;
            }

            try
            {
                UnregisterApkAssociation();
                try { Registry.CurrentUser.DeleteSubKeyTree(UninstallSubKey, false); } catch { }
                TryDeleteFile(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.Programs), "AAOW App Hub.lnk"));
                TryDeleteFile(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory), "AAOW App Hub.lnk"));

                if (Directory.Exists(installDir))
                {
                    string self = Path.GetFullPath(Assembly.GetExecutingAssembly().Location);
                    if (self.StartsWith(Path.GetFullPath(installDir), StringComparison.OrdinalIgnoreCase))
                    {
                        // Tự xóa thư mục sau khi tiến trình kết thúc
                        ProcessStartInfo psi = new ProcessStartInfo("cmd.exe",
                            "/C timeout /t 2 /nobreak >nul & rmdir /S /Q \"" + installDir + "\"");
                        psi.CreateNoWindow = true;
                        psi.UseShellExecute = false;
                        Process.Start(psi);
                    }
                    else
                    {
                        Directory.Delete(installDir, true);
                    }
                }

                if (!silent)
                {
                    MessageBox.Show("Đã gỡ cài đặt AAOW thành công.", "Gỡ cài đặt AAOW",
                        MessageBoxButtons.OK, MessageBoxIcon.Information);
                }
                return 0;
            }
            catch (Exception ex)
            {
                if (!silent)
                {
                    MessageBox.Show("Lỗi khi gỡ cài đặt: " + ex.Message, "Gỡ cài đặt AAOW",
                        MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
                return 1;
            }
        }

        private static void RegisterApkAssociation(string exePath, string iconSpec)
        {
            using (RegistryKey classes = Registry.CurrentUser.CreateSubKey(@"Software\Classes"))
            {
                using (RegistryKey extKey = classes.CreateSubKey(".apk"))
                {
                    extKey.SetValue("", ProgId);
                    extKey.SetValue("Content Type", "application/vnd.android.package-archive");
                }
                using (RegistryKey progKey = classes.CreateSubKey(ProgId))
                {
                    progKey.SetValue("", "Android Application Package (AAOW)");
                    using (RegistryKey defIcon = progKey.CreateSubKey("DefaultIcon"))
                    {
                        defIcon.SetValue("", iconSpec);
                    }
                    using (RegistryKey openKey = progKey.CreateSubKey(@"shell\open"))
                    {
                        openKey.SetValue("", "Mở bằng AAOW");
                        using (RegistryKey cmd = openKey.CreateSubKey("command"))
                        {
                            cmd.SetValue("", "\"" + exePath + "\" \"%1\"");
                        }
                    }
                    using (RegistryKey installKey = progKey.CreateSubKey(@"shell\install"))
                    {
                        installKey.SetValue("", "Cài vào AAOW App Hub");
                        using (RegistryKey cmd = installKey.CreateSubKey("command"))
                        {
                            cmd.SetValue("", "\"" + exePath + "\" --install \"%1\"");
                        }
                    }
                }
            }
            try { SHChangeNotify(0x08000000, 0, IntPtr.Zero, IntPtr.Zero); } catch { }
        }

        private static void UnregisterApkAssociation()
        {
            using (RegistryKey classes = Registry.CurrentUser.OpenSubKey(@"Software\Classes", true))
            {
                if (classes == null) return;
                using (RegistryKey extKey = classes.OpenSubKey(".apk"))
                {
                    if (extKey != null && string.Equals(Convert.ToString(extKey.GetValue("")), ProgId, StringComparison.OrdinalIgnoreCase))
                    {
                        extKey.Close();
                        try { classes.DeleteSubKeyTree(".apk", false); } catch { }
                    }
                }
                try { classes.DeleteSubKeyTree(ProgId, false); } catch { }
            }
            try { SHChangeNotify(0x08000000, 0, IntPtr.Zero, IntPtr.Zero); } catch { }
        }

        private static void RegisterUninstallEntry(string installDir, string exePath, string uninstallerPath, string iconPath)
        {
            using (RegistryKey key = Registry.CurrentUser.CreateSubKey(UninstallSubKey))
            {
                key.SetValue("DisplayName", AppDisplayName);
                key.SetValue("DisplayVersion", AppVersion);
                key.SetValue("Publisher", "AAOW Project");
                key.SetValue("InstallLocation", installDir);
                key.SetValue("DisplayIcon", File.Exists(iconPath) ? iconPath : exePath + ",0");
                key.SetValue("UninstallString", "\"" + uninstallerPath + "\" /UNINSTALL /D=\"" + installDir + "\"");
                key.SetValue("QuietUninstallString", "\"" + uninstallerPath + "\" /UNINSTALL /S /D=\"" + installDir + "\"");
                key.SetValue("NoModify", 1, RegistryValueKind.DWord);
                key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
            }
        }

        private static void CreateShortcut(string shortcutPath, string targetExe, string workDir, string iconPath, string desc)
        {
            try
            {
                Type shellType = Type.GetTypeFromProgID("WScript.Shell");
                if (shellType == null) return;
                object shell = Activator.CreateInstance(shellType);
                object shortcut = shellType.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod, null, shell, new object[] { shortcutPath });
                Type scType = shortcut.GetType();
                scType.InvokeMember("TargetPath", BindingFlags.SetProperty, null, shortcut, new object[] { targetExe });
                scType.InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, shortcut, new object[] { workDir });
                scType.InvokeMember("Description", BindingFlags.SetProperty, null, shortcut, new object[] { desc });
                if (File.Exists(iconPath))
                {
                    scType.InvokeMember("IconLocation", BindingFlags.SetProperty, null, shortcut, new object[] { iconPath });
                }
                scType.InvokeMember("Save", BindingFlags.InvokeMethod, null, shortcut, null);
            }
            catch { }
        }

        private static void ExtractResourceToFile(string resourceName, string destPath)
        {
            Assembly asm = Assembly.GetExecutingAssembly();
            using (Stream s = asm.GetManifestResourceStream(resourceName))
            {
                if (s == null) throw new FileNotFoundException("Missing embedded resource: " + resourceName);
                using (FileStream fs = File.Create(destPath))
                {
                    s.CopyTo(fs);
                }
            }
        }

        private static void TryExtractResourceToFile(string resourceName, string destPath)
        {
            try { ExtractResourceToFile(resourceName, destPath); } catch { }
        }

        private static void TryDeleteFile(string path)
        {
            try { if (File.Exists(path)) File.Delete(path); } catch { }
        }

        private static void Report(Action<int, string> progress, int pct, string msg)
        {
            if (progress != null) progress(pct, msg);
        }
    }

    internal sealed class SetupForm : Form
    {
        private readonly TextBox txtDir;
        private readonly CheckBox chkAssoc;
        private readonly CheckBox chkShortcut;
        private readonly CheckBox chkLaunch;
        private readonly ProgressBar progressBar;
        private readonly Label lblStatus;
        private readonly Button btnInstall;
        private readonly Button btnBrowse;
        private bool finished;

        public SetupForm(string defaultDir)
        {
            Text = "Cài đặt AAOW v1.0.0 — Android Apps On Windows";
            Size = new Size(540, 360);
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            BackColor = Color.FromArgb(15, 17, 26);
            ForeColor = Color.FromArgb(242, 244, 250);
            Font = new Font("Segoe UI", 9.5f);

            try
            {
                using (Stream ico = Assembly.GetExecutingAssembly().GetManifestResourceStream("aaow.ico"))
                {
                    if (ico != null) Icon = new Icon(ico);
                }
            }
            catch { }

            Label lblTitle = new Label();
            lblTitle.Text = "AAOW — Android Apps On Windows";
            lblTitle.Font = new Font("Segoe UI", 14f, FontStyle.Bold);
            lblTitle.ForeColor = Color.White;
            lblTitle.SetBounds(22, 18, 480, 30);
            Controls.Add(lblTitle);

            Label lblSub = new Label();
            lblSub.Text = "Trình chạy file .apk trực tiếp trên Windows (Java 21 Compatibility Layer, không máy ảo).";
            lblSub.ForeColor = Color.FromArgb(154, 162, 194);
            lblSub.SetBounds(22, 48, 480, 22);
            Controls.Add(lblSub);

            Label lblDir = new Label();
            lblDir.Text = "Thư mục cài đặt:";
            lblDir.SetBounds(22, 86, 150, 20);
            Controls.Add(lblDir);

            txtDir = new TextBox();
            txtDir.Text = defaultDir;
            txtDir.BackColor = Color.FromArgb(31, 35, 53);
            txtDir.ForeColor = Color.White;
            txtDir.BorderStyle = BorderStyle.FixedSingle;
            txtDir.SetBounds(22, 110, 402, 25);
            Controls.Add(txtDir);

            btnBrowse = new Button();
            btnBrowse.Text = "Chọn...";
            btnBrowse.FlatStyle = FlatStyle.Flat;
            btnBrowse.BackColor = Color.FromArgb(43, 49, 75);
            btnBrowse.ForeColor = Color.White;
            btnBrowse.SetBounds(432, 109, 74, 26);
            btnBrowse.Click += delegate
            {
                using (FolderBrowserDialog fbd = new FolderBrowserDialog())
                {
                    fbd.SelectedPath = txtDir.Text;
                    if (fbd.ShowDialog(this) == DialogResult.OK)
                    {
                        txtDir.Text = fbd.SelectedPath;
                    }
                }
            };
            Controls.Add(btnBrowse);

            chkAssoc = new CheckBox();
            chkAssoc.Text = "Liên kết đuôi file .apk (nhấp đúp file .apk trong Explorer để mở bằng AAOW)";
            chkAssoc.Checked = true;
            chkAssoc.SetBounds(22, 152, 485, 24);
            Controls.Add(chkAssoc);

            chkShortcut = new CheckBox();
            chkShortcut.Text = "Tạo lối tắt trên Desktop và Start Menu";
            chkShortcut.Checked = true;
            chkShortcut.SetBounds(22, 180, 485, 24);
            Controls.Add(chkShortcut);

            chkLaunch = new CheckBox();
            chkLaunch.Text = "Mở AAOW App Hub ngay sau khi cài đặt";
            chkLaunch.Checked = true;
            chkLaunch.SetBounds(22, 208, 485, 24);
            Controls.Add(chkLaunch);

            progressBar = new ProgressBar();
            progressBar.SetBounds(22, 246, 484, 18);
            progressBar.Minimum = 0;
            progressBar.Maximum = 100;
            Controls.Add(progressBar);

            lblStatus = new Label();
            lblStatus.Text = "Sẵn sàng cài đặt (đã tích hợp sẵn Java 21 Runtime rút gọn).";
            lblStatus.ForeColor = Color.FromArgb(156, 231, 184);
            lblStatus.SetBounds(22, 270, 360, 22);
            Controls.Add(lblStatus);

            btnInstall = new Button();
            btnInstall.Text = "Cài đặt";
            btnInstall.FlatStyle = FlatStyle.Flat;
            btnInstall.BackColor = Color.FromArgb(88, 101, 242);
            btnInstall.ForeColor = Color.White;
            btnInstall.Font = new Font("Segoe UI", 9.5f, FontStyle.Bold);
            btnInstall.SetBounds(396, 272, 110, 32);
            btnInstall.Click += OnInstallClick;
            Controls.Add(btnInstall);
        }

        private void OnInstallClick(object sender, EventArgs e)
        {
            if (finished)
            {
                if (chkLaunch.Checked)
                {
                    string exe = Path.Combine(txtDir.Text.Trim(), "AAOW.exe");
                    if (File.Exists(exe))
                    {
                        Process.Start(new ProcessStartInfo(exe) { WorkingDirectory = txtDir.Text.Trim(), UseShellExecute = true });
                    }
                }
                Close();
                return;
            }

            string dir = txtDir.Text.Trim();
            bool doShortcut = chkShortcut.Checked;
            bool doAssoc = chkAssoc.Checked;
            btnInstall.Enabled = false;
            btnBrowse.Enabled = false;
            txtDir.Enabled = false;

            Thread worker = new Thread(delegate()
            {
                try
                {
                    Program.PerformInstall(dir, doShortcut, doAssoc, delegate(int pct, string msg)
                    {
                        BeginInvoke((Action)delegate
                        {
                            progressBar.Value = Math.Max(0, Math.Min(100, pct));
                            lblStatus.Text = msg;
                        });
                    });
                    BeginInvoke((Action)delegate
                    {
                        finished = true;
                        btnInstall.Text = "Hoàn tất";
                        btnInstall.Enabled = true;
                    });
                }
                catch (Exception ex)
                {
                    BeginInvoke((Action)delegate
                    {
                        lblStatus.ForeColor = Color.FromArgb(255, 138, 128);
                        lblStatus.Text = "Lỗi: " + ex.Message;
                        btnInstall.Enabled = true;
                        btnBrowse.Enabled = true;
                        txtDir.Enabled = true;
                    });
                }
            });
            worker.IsBackground = true;
            worker.Start();
        }
    }
}
