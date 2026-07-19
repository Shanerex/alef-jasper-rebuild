"use client";

import { useState } from "react";
import { changePassword } from "@/lib/api/admin-account";
import { AdminApiError } from "@/lib/api/admin-client";
import { AdminField } from "@/components/admin/ui/admin-field";
import { AdminInput } from "@/components/admin/ui/admin-input";
import { FormErrorBanner } from "@/components/admin/admin-page-header";

/**
 * Account -- change password screen (design.md §B.9, F12-AC28, F12-AC29).
 *
 * Single centered form card, single-column at every width (already
 * responsive by construction -- §B.10). No "forgot password" affordance --
 * a fully forgotten password is an ops action (§A.7).
 */
export default function AdminAccountPage() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [banner, setBanner] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  /** Client-side validation before submit (design §B.9): mirrors the server policy for instant feedback. */
  function validate(): Record<string, string> {
    const errors: Record<string, string> = {};
    if (newPassword.length < 12) errors.newPassword = "At least 12 characters.";
    if (newPassword && newPassword === currentPassword) {
      errors.newPassword = "Must differ from the current password.";
    }
    if (newPassword !== confirmPassword) errors.confirmPassword = "Passwords do not match.";
    return errors;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSuccess(false);
    setBanner(null);

    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});
    setSubmitting(true);

    try {
      await changePassword({ currentPassword, newPassword });
      setSuccess(true);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      if (err instanceof AdminApiError && err.status === 400 && err.problem?.fields) {
        const mapped: Record<string, string> = {};
        for (const [field, messages] of Object.entries(err.problem.fields)) {
          mapped[field] = messages.join(" ");
        }
        setFieldErrors(mapped);
      } else if (err instanceof AdminApiError && err.status === 403) {
        setBanner("Session problem, please sign in again.");
      } else {
        setBanner("Something went wrong, please retry.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="max-w-[420px]">
      <h1 className="mb-6 font-serif text-[28px] font-normal text-text-primary">Account</h1>

      <FormErrorBanner message={banner} />

      {success && (
        <div className="mb-5 border-l-2 border-gold bg-gold-ghost px-4 py-3">
          <p className="font-sans text-[12px] text-text-primary">
            Password changed. Use it next time you sign in.
          </p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <AdminField id="current-password" label="Current Password" required error={fieldErrors.currentPassword}>
          <AdminInput
            id="current-password"
            type="password"
            autoComplete="current-password"
            required
            disabled={submitting}
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />
        </AdminField>

        <AdminField
          id="new-password"
          label="New Password"
          required
          error={fieldErrors.newPassword}
          hint="At least 12 characters."
        >
          <AdminInput
            id="new-password"
            type="password"
            autoComplete="new-password"
            required
            disabled={submitting}
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />
        </AdminField>

        <AdminField id="confirm-password" label="Confirm New Password" required error={fieldErrors.confirmPassword}>
          <AdminInput
            id="confirm-password"
            type="password"
            autoComplete="new-password"
            required
            disabled={submitting}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />
        </AdminField>

        <button
          type="submit"
          disabled={submitting}
          className="mt-2 bg-gold px-6 py-3 font-sans text-[11px] font-bold uppercase tracking-[0.16em] text-page hover:bg-gold-light disabled:opacity-50"
        >
          {submitting ? "Changing…" : "Change Password"}
        </button>
      </form>
    </div>
  );
}
