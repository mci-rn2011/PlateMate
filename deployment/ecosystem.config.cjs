const appRoot = process.env.PLATEMATE_APP_ROOT || "C:\\apps\\platemate";
const repoPath = process.env.PLATEMATE_REPO_PATH || `${appRoot}\\repo`;
const runtimePath = process.env.PLATEMATE_RUNTIME_PATH || `${appRoot}\\current`;

module.exports = {
  apps: [
    {
      name: "platemate",
      script: "java",
      args: `-jar ${runtimePath}\\platemate.jar`,
      cwd: runtimePath,
      env: {
        SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE || "prod",
        PORT: process.env.PORT || "8081",
        PLATEMATE_DB_URL:
          process.env.PLATEMATE_DB_URL || "jdbc:postgresql://localhost:5432/platemate",
        PLATEMATE_DB_USERNAME: process.env.PLATEMATE_DB_USERNAME || "platemate",
        PLATEMATE_DB_PASSWORD: process.env.PLATEMATE_DB_PASSWORD || "CHANGE_ME",
        PLATEMATE_UPLOAD_ROOT:
          process.env.PLATEMATE_UPLOAD_ROOT || `${appRoot}\\uploads`,
        MAPBOX_ACCESS_TOKEN: process.env.MAPBOX_ACCESS_TOKEN || ""
      }
    },
    {
      name: "platemate-webhook",
      script: `${repoPath}\\deployment\\webhook-server.js`,
      cwd: repoPath,
      env: {
        PLATEMATE_WEBHOOK_PORT: process.env.PLATEMATE_WEBHOOK_PORT || "9091",
        PLATEMATE_WEBHOOK_SECRET:
          process.env.PLATEMATE_WEBHOOK_SECRET || "CHANGE_ME",
        PLATEMATE_DEPLOY_SCRIPT:
          process.env.PLATEMATE_DEPLOY_SCRIPT || `${repoPath}\\deployment\\deploy.ps1`,
        PLATEMATE_DEPLOY_BRANCH: process.env.PLATEMATE_DEPLOY_BRANCH || "main"
      }
    }
  ]
};
