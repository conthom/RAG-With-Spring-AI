import { NextResponse } from "next/server";

const springApiUrl = (process.env.SPRING_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

export async function forwardToSpring(path: string, init: RequestInit) {
  try {
    const response = await fetch(`${springApiUrl}${path}`, { ...init, cache: "no-store" });
    const body = await response.json().catch(() => ({ message: "Spring API returned an unreadable response." }));
    return NextResponse.json(body, { status: response.status });
  } catch {
    return NextResponse.json(
      { message: "Could not reach the Spring API. Check that it is running on port 8080." },
      { status: 502 },
    );
  }
}