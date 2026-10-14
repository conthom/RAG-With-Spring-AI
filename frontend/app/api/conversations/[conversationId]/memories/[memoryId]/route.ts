import { forwardToSpring } from "@/lib/spring-api";

type RouteContext = { params: Promise<{ conversationId: string; memoryId: string }> };

export async function PUT(request: Request, context: RouteContext) {
  const { conversationId, memoryId } = await context.params;
  return forwardToSpring(
    `/api/conversations/${encodeURIComponent(conversationId)}/memories/${encodeURIComponent(memoryId)}`,
    {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: await request.text(),
    },
  );
}

export async function DELETE(_request: Request, context: RouteContext) {
  const { conversationId, memoryId } = await context.params;
  return forwardToSpring(
    `/api/conversations/${encodeURIComponent(conversationId)}/memories/${encodeURIComponent(memoryId)}`,
    { method: "DELETE" },
  );
}