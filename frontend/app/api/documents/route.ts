import { forwardToSpring } from "@/lib/spring-api";

export async function POST(request: Request) {
  return forwardToSpring("/api/documents", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text(),
  });
}