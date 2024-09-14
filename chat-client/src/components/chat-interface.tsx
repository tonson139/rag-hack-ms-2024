import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { SendIcon, UserIcon } from "lucide-react";

const getAIResponse = async (message: string): Promise<string> => {
    const myHeaders = new Headers();
    myHeaders.append("Content-Type", "application/json");
    return new Promise((resolve, reject) => {
        fetch("http://localhost:8080/ai/generate-response", {
            method: "POST",
            headers: myHeaders,
            body: JSON.stringify({ message }),
        })
            .then((response) => {
                if (!response.ok) {
                    reject(
                        new Error(
                            `Request failed with status ${response.status}`
                        )
                    );
                }
                return response.text();
            })
            .then((result) => {
                resolve(result);
            })
            .catch((error) => {
                reject(error);
            });
    });
};

export function ChatInterfaceComponent() {
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
                    {" "}
                    Political Science TA
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
                <Button type="submit" disabled={isLoading}>
                    {isLoading ? (
                        "Sending..."
                    ) : (
                        <SendIcon className="h-4 w-4" />
                    )}
                </Button>
            </form>
        </div>
    );
}
