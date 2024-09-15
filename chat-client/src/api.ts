export async function getAIResponse(message: string): Promise<string> {
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
}
