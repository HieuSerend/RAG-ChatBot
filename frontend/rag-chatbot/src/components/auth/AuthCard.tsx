import React from "react";

type Props = {
  title: string;
  children: React.ReactNode;
};

export default function AuthCard({ title, children }: Props) {
  return (
    <div className="w-[380px] bg-white p-7 rounded-xl shadow-[0_8px_30px_rgba(10,10,10,0.07)] text-center">
      <h2 className="m-0 mb-3 text-lg text-slate-900 font-semibold">{title}</h2>
      <div className="mt-1">{children}</div>
    </div>
  );
}
