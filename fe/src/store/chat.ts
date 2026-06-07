import { create } from "zustand";

interface ChatStore {
  isOpen: boolean;
  orderCode: string | null;
  openChat: (orderCode: string) => void;
  closeChat: () => void;
}

export const useChatStore = create<ChatStore>((set) => ({
  isOpen: false,
  orderCode: null,
  openChat: (orderCode) => set({ isOpen: true, orderCode }),
  closeChat: () => set({ isOpen: false, orderCode: null }),
}));
