/**
 * 로컬 프론트엔드용 정적 파일 서버 (Node 기본 모듈만 사용)
 *
 * `python -m http.server` 는 Range 요청(부분 전송)을 지원하지 않아 항상 200 + 전체 본문을 돌려준다.
 * 그러면 브라우저가 동영상의 원하는 위치를 요청할 수 없어서 **재생 위치를 앞뒤로 못 옮긴다**
 * (소개 영상에서 00:03 → 00:10 으로 넘어가지지 않던 원인, 2026-09-28).
 * 이 서버는 `Range: bytes=...` 를 받아 206 Partial Content 로 응답한다 — 운영의 nginx 와 같은 동작.
 *
 *   node scripts/static-server.js <문서루트> [포트] [바인드주소]
 */
const http = require("http");
const fs = require("fs");
const path = require("path");
const url = require("url");

const root = path.resolve(process.argv[2] || process.cwd());
const port = Number(process.argv[3] || 4000);
const host = process.argv[4] || "127.0.0.1";

const mimeTypes = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".geojson": "application/geo+json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".gif": "image/gif",
  ".webp": "image/webp",
  ".ico": "image/x-icon",
  ".mp4": "video/mp4",
  ".m4a": "audio/mp4",
  ".mp3": "audio/mpeg",
  ".webm": "video/webm",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".txt": "text/plain; charset=utf-8",
  ".md": "text/markdown; charset=utf-8",
};

/** 문서 루트를 벗어나는 경로 요청을 막는다 */
function resolveSafePath(requestPath) {
  const decoded = decodeURIComponent(requestPath.split("?")[0]);
  const target = path.join(root, path.normalize(decoded).replace(/^([/\\])+/, ""));
  if (target !== root && !target.startsWith(root + path.sep)) return null;
  return target;
}

function sendError(res, status, message) {
  res.writeHead(status, { "Content-Type": "text/plain; charset=utf-8" });
  res.end(message);
}

const server = http.createServer((req, res) => {
  const requestPath = url.parse(req.url).pathname;
  let filePath = resolveSafePath(requestPath);
  if (!filePath) return sendError(res, 403, "Forbidden");

  fs.stat(filePath, (err, stat) => {
    if (!err && stat.isDirectory()) {
      filePath = path.join(filePath, "index.html");
      return serveFile(filePath);
    }
    if (err) return sendError(res, 404, "Not Found: " + requestPath);
    serveFile(filePath, stat);
  });

  function serveFile(target, knownStat) {
    const finish = (stat) => {
      const ext = path.extname(target).toLowerCase();
      const type = mimeTypes[ext] || "application/octet-stream";
      const headers = {
        "Content-Type": type,
        "Last-Modified": stat.mtime.toUTCString(),
        // 로컬에서는 항상 최신 파일을 보도록 캐시하지 않는다(?v= 를 안 붙여도 반영됨)
        "Cache-Control": "no-cache",
        // 동영상·오디오 위치 이동(seek)에 필요하다는 것을 브라우저에 알린다
        "Accept-Ranges": "bytes",
      };

      const range = req.headers.range;
      if (range) {
        const match = /^bytes=(\d*)-(\d*)$/.exec(range.trim());
        if (match) {
          let start = match[1] === "" ? null : Number(match[1]);
          let end = match[2] === "" ? null : Number(match[2]);
          if (start === null) {
            // "bytes=-500" → 마지막 500바이트
            start = Math.max(0, stat.size - (end || 0));
            end = stat.size - 1;
          } else if (end === null || end >= stat.size) {
            end = stat.size - 1;
          }
          if (start > end || start >= stat.size) {
            res.writeHead(416, { "Content-Range": "bytes */" + stat.size });
            return res.end();
          }
          headers["Content-Range"] = "bytes " + start + "-" + end + "/" + stat.size;
          headers["Content-Length"] = end - start + 1;
          res.writeHead(206, headers);
          if (req.method === "HEAD") return res.end();
          return fs.createReadStream(target, { start, end }).pipe(res);
        }
      }

      headers["Content-Length"] = stat.size;
      res.writeHead(200, headers);
      if (req.method === "HEAD") return res.end();
      fs.createReadStream(target).pipe(res);
    };

    if (knownStat) return finish(knownStat);
    fs.stat(target, (statErr, stat) => {
      if (statErr || !stat.isFile()) return sendError(res, 404, "Not Found: " + requestPath);
      finish(stat);
    });
  }
});

server.listen(port, host, () => {
  console.log("정적 서버 기동: http://" + host + ":" + port + " (문서 루트 " + root + ")");
  console.log("Range 요청 지원 — 동영상 위치 이동(seek) 가능");
});
