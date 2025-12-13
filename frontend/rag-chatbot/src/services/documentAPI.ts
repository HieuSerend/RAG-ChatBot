import api from "./api";

const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

export const uploadDocument = async (file: File): Promise<any> => {
  // Validate file type
  if (file.type !== "application/pdf") {
    throw new Error("Only PDF files are allowed.");
  }

  // Validate file size
  if (file.size > MAX_FILE_SIZE) {
    throw new Error("File size exceeds 50MB limit.");
  }

  const formData = new FormData();
  formData.append("file", file);

  const response = await api.post("/documents/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return response.data.data;
};
