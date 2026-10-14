import { forwardToSpring } from "@/lib/spring-api";

export async function GET(
  _request: Request,
  context: { params: Promise<{ conversationId: string }> },
) {
  const { conversationId } = await context.params;
  return forwardToSpring(`/api/conversations/${encodeURIComponent(conversationId)}`, {
    method: "GET",
  });
}