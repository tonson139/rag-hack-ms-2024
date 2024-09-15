import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Loader2, SendIcon, UserIcon } from "lucide-react";
import { getAIResponse } from "../api";

export function ChatRoom() {
    const [messages, setMessages] = useState<
        Array<{ role: "user" | "ai"; content: string }>
    >([]);
    const [inputMessage, setInputMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);

    const handleSendMessage = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!inputMessage.trim()) return;

        const userMessage = { role: "user" as const, content: inputMessage };
        setMessages((prev) => [...prev, userMessage]);
        setInputMessage("");
        setIsLoading(true);

        try {
            const aiResponse = await getAIResponse(inputMessage);
            setMessages((prev) => [
                ...prev,
                { role: "ai", content: aiResponse },
            ]);
        } catch (error) {
            console.error("Failed to get AI response:", error);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col h-screen max-w-5xl mx-auto">
            <header className="p-4 border-b bg-slate-900">
                <h1 className="text-2xl font-bold text-center text-white">
                    Philosophy and Political science
                </h1>
            </header>
            <ScrollArea className="flex-grow p-4">
                {messages.map((message, index) => (
                    <div
                        key={index}
                        className={`flex items-start mb-4 ${
                            message.role === "ai"
                                ? "justify-start"
                                : "justify-end"
                        }`}
                    >
                        <Avatar
                            className={`${
                                message.role === "ai" ? "mr-2" : "ml-2 order-2"
                            }`}
                        >
                            <AvatarFallback>
                                {message.role === "ai" ? "AI" : "You"}
                            </AvatarFallback>
                            {message.role === "ai" ? (
                                <AvatarImage src="/placeholder.svg?height=40&width=40" />
                            ) : (
                                <UserIcon className="p-1" />
                            )}
                        </Avatar>
                        <div
                            className={`rounded-lg p-3 max-w-[80%] ${
                                message.role === "ai"
                                    ? "bg-muted"
                                    : "bg-primary text-primary-foreground"
                            }`}
                        >
                            {message.content}
                        </div>
                    </div>
                ))}
            </ScrollArea>
            <form
                onSubmit={handleSendMessage}
                className="p-4 border-t flex items-center"
            >
                <Input
                    type="text"
                    placeholder="Type your message..."
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    className="flex-grow mr-2"
                />
                <Button
                    type="submit"
                    disabled={isLoading}
                    size="lg"

                    // className="text-gray-900  borderfocus:outline-none hover:bg-gray-100 focus:ring-4 focus:ring-gray-100 font-medium rounded-full text-sm px-5 py-2.5 me-2 mb-2 dark:bg-gray-800 dark:text-white dark:border-gray-600 dark:hover:bg-gray-700 dark:hover:border-gray-600 dark:focus:ring-gray-700"
                >
                    {isLoading ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            Sending...
                        </>
                    ) : (
                        <SendIcon className="h-4 w-4" />
                    )}
                </Button>
            </form>
        </div>
    );
}
