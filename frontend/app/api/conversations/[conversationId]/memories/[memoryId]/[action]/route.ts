import { NextResponse } from "next/server";
import { forwardToSpring } from "@/lib/spring-api";

type RouteContext = {
  params: Promise<{ conversationId: string; memoryId: string; action: string }>;
};

const actions = new Set(["approve", "reject", "revoke"]);

export async function POST(_request: Request, context: RouteContext) {
  const { conversationId, memoryId, action } = await context.params;
  if (!actions.has(action)) {
    return NextResponse.json({ message: "Unknown memory action." }, { status: 404 });
  }

  return forwardToSpring(
    `/api/conversations/${encodeURIComponent(conversationId)}/memories/${encodeURIComponent(memoryId)}/${action}`,
    { method: "POST" },
  );
}