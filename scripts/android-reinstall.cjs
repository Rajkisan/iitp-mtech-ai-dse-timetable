const { spawnSync } = require("child_process");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const npmCommand = process.platform === "win32" ? "npm.cmd" : "npm";

spawnSync(npmCommand, ["run", "android:uninstall"], {
  cwd: rootDir,
  stdio: "inherit"
});

const install = spawnSync(npmCommand, ["run", "android:install"], {
  cwd: rootDir,
  stdio: "inherit"
});

process.exit(install.status || 0);
