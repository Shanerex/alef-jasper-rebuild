"use client";

import { Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { login } from "@/lib/api/admin-session";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput } from "@/components/admin/ui/admin-input";

/**
 * Admin login screen (design.md §B.2, F12-AC1).
 *
 * Thin wrapper around LoginForm. Required because LoginForm calls
 * useSearchParams() (to read the post-login `?next=` redirect target) --
 * Next.js's App Router requires any useSearchParams() consumer to be
 * wrapped in a <Suspense> boundary, or `next build`'s static prerendering
 * step fails outright (bug fix, post-QA: handoffs/5-qa-to-dev.md Bug #2).
 * Follows the same pattern already established in
 * web/src/app/projects/page.tsx for the same underlying requirement.
 */
export default function AdminLoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}

/**
 * The actual login screen: single centered card, naturally responsive
 * (scales from phone to desktop via horizontal padding + a max card width --
 * §B.10). No "forgot password" affordance in v1 (a fully forgotten password
 * is an ops action -- §A.7).
 *
 * Split out from AdminLoginPage so the useSearchParams() call has its own
 * Suspense boundary (see AdminLoginPage's doc comment).
 */
function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login({ username, password });
      const next = searchParams.get("next") || "/admin";
      router.replace(next);
    } catch (err) {
      if (err instanceof AdminApiError && err.status === 429) {
        setError("Too many attempts, try again later.");
      } else {
        setError("Username or password is incorrect.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-page px-4">
      <div className="w-full max-w-[380px] border border-gold-subtle bg-surface-2 p-8">
        <h1 className="mb-6 font-sans text-[16px] font-bold uppercase tracking-[0.14em] text-text-primary">
          ALEF Admin
        </h1>

        {error && (
          <div role="alert" className="mb-5 border-l-2 border-gold bg-gold-ghost px-4 py-3">
            <p className="font-sans text-[12px] text-text-primary">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <AdminField id="login-username" label="Username" required>
            <AdminInput
              id="login-username"
              autoComplete="username"
              required
              disabled={submitting}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </AdminField>

          <AdminField id="login-password" label="Password" required>
            <AdminInput
              id="login-password"
              type="password"
              autoComplete="current-password"
              required
              disabled={submitting}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </AdminField>

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 bg-gold px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-page hover:bg-gold-light disabled:opacity-50"
          >
            {submitting ? "Signing In…" : "Sign In"}
          </button>
        </form>
      </div>
    </div>
  );
}
