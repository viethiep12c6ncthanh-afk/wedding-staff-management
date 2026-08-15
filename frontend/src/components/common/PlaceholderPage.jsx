function PlaceholderPage({ title, description }) {
    return (
        <section>
            <div className="page-heading">
                <div>
                    <h1>{title}</h1>
                    <p>{description}</p>
                </div>
            </div>

            <div className="placeholder-card">
                <h2>{title}</h2>

                <p>
                    Giao diện của chức năng này sẽ được hoàn thiện
                    trong các commit tiếp theo.
                </p>
            </div>
        </section>
    );
}

export default PlaceholderPage;