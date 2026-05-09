const crypto = require("node:crypto");
const http = require("node:http");
const { execFile } = require("node:child_process");

const PORT = Number(process.env.PLATEMATE_WEBHOOK_PORT || 9091);
const SECRET = process.env.PLATEMATE_WEBHOOK_SECRET || "";
const DEPLOY_SCRIPT = process.env.PLATEMATE_DEPLOY_SCRIPT || "C:\\apps\\platemate\\repo\\deployment\\deploy.ps1";
const DEPLOY_BRANCH = process.env.PLATEMATE_DEPLOY_BRANCH || "main";

let deploymentRunning = false;

function verifySignature(rawBody, signatureHeader) {
  if (!SECRET) {
    return false;
  }

  const expected = `sha256=${crypto
    .createHmac("sha256", SECRET)
    .update(rawBody)
    .digest("hex")}`;

  return crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signatureHeader || ""));
}

function runDeploy() {
  if (deploymentRunning) {
    return;
  }

  deploymentRunning = true;
  execFile(
    "powershell.exe",
    ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", DEPLOY_SCRIPT],
    { windowsHide: true },
    (error, stdout, stderr) => {
      deploymentRunning = false;
      console.log(stdout);
      if (stderr) {
        console.error(stderr);
      }
      if (error) {
        console.error(error);
      }
    }
  );
}

const server = http.createServer((request, response) => {
  if (request.method !== "POST" || request.url !== "/github") {
    response.writeHead(404);
    response.end("not found");
    return;
  }

  const chunks = [];
  request.on("data", (chunk) => chunks.push(chunk));
  request.on("end", () => {
    const rawBody = Buffer.concat(chunks);
    const signature = request.headers["x-hub-signature-256"];

    if (!verifySignature(rawBody, signature)) {
      response.writeHead(401);
      response.end("invalid signature");
      return;
    }

    const payload = JSON.parse(rawBody.toString("utf8"));
    if (payload.ref !== `refs/heads/${DEPLOY_BRANCH}`) {
      response.writeHead(202);
      response.end("ignored branch");
      return;
    }

    runDeploy();
    response.writeHead(202);
    response.end("deployment queued");
  });
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`PlateMate webhook listening on http://127.0.0.1:${PORT}/github`);
});
