import { forwardToSpring } from "@/lib/spring-api";

type RouteContext = { params: Promise<{ conversationId: string }> };

export async function GET(_request: Request, context: RouteContext) {
  const { conversationId } = await context.params;
  return forwardToSpring(`/api/conversations/${encodeURIComponent(conversationId)}/memories`, {
    method: "GET",
  });
}

export async function POST(request: Request, context: RouteContext) {
  const { conversationId } = await context.params;
  return forwardToSpring(`/api/conversations/${encodeURIComponent(conversationId)}/memories`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: await request.text(),
  });
}